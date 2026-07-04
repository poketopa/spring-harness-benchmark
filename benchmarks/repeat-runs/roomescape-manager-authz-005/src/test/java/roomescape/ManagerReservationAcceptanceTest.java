package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.repository.MemberRepository;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationAcceptanceTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Test
    @DisplayName("매니저는 자기 매장 예약만 조회할 수 있다")
    void managerFindsOnlyOwnStoreReservations() {
        // given
        String managerToken = createManagerAndLogin("매니저", "manager@example.com");
        String otherManagerToken = createManagerAndLogin("다른매니저", "other-manager@example.com");
        long ownThemeId = createStoreTheme("강남점", "강남 방", "manager@example.com");
        long otherThemeId = createStoreTheme("홍대점", "홍대 방", "other-manager@example.com");
        long timeId = createTime(LocalTime.of(10, 0));
        String brownToken = createMemberAndLogin("브라운", "brown@example.com");
        String conyToken = createMemberAndLogin("코니", "cony@example.com");
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, ownThemeId);
        createReservation(conyToken, LocalDate.of(2030, 5, 2), timeId, otherThemeId);

        // when
        ResponseEntity<List> ownResponse = findManagedReservations(managerToken);
        ResponseEntity<List> otherResponse = findManagedReservations(otherManagerToken);

        // then
        assertThat(ownResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownResponse.getBody()).hasSize(1);
        assertThat((String) ((Map<?, ?>) ownResponse.getBody().getFirst()).get("themeName")).isEqualTo("강남 방");

        assertThat(otherResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(otherResponse.getBody()).hasSize(1);
        assertThat((String) ((Map<?, ?>) otherResponse.getBody().getFirst()).get("themeName")).isEqualTo("홍대 방");
    }

    @Test
    @DisplayName("매니저는 자기 매장 예약을 변경하고 취소할 수 있다")
    void managerChangesAndCancelsOwnStoreReservation() {
        // given
        String managerToken = createManagerAndLogin("매니저", "manager@example.com");
        long themeId = createStoreTheme("강남점", "강남 방", "manager@example.com");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        String brownToken = createMemberAndLogin("브라운", "brown@example.com");
        long reservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        // when
        ResponseEntity<Map> changed = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );
        ResponseEntity<Void> canceled = cancelManagedReservation(managerToken, reservationId);

        // then
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) elevenOClockId);
        assertThat(canceled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(brownToken)).isEmpty();
    }

    @Test
    @DisplayName("다른 매장 예약에 접근한 매니저는 인가 실패 응답을 받는다")
    void managerCannotAccessOtherStoreReservation() {
        // given
        String managerToken = createManagerAndLogin("매니저", "manager@example.com");
        createManagerAndLogin("다른매니저", "other-manager@example.com");
        createStoreTheme("강남점", "강남 방", "manager@example.com");
        long otherThemeId = createStoreTheme("홍대점", "홍대 방", "other-manager@example.com");
        long timeId = createTime(LocalTime.of(10, 0));
        String brownToken = createMemberAndLogin("브라운", "brown@example.com");
        long reservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                timeId,
                otherThemeId
        ).getBody().get("id")).longValue();

        // when
        ResponseEntity<Map> response = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                timeId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 요청과 매니저가 아닌 회원 요청은 인증 실패와 인가 실패로 구분된다")
    void authenticationAndAuthorizationFailuresAreSeparated() {
        // given
        String userToken = createMemberAndLogin("브라운", "brown@example.com");

        // when
        ResponseEntity<Map> unauthenticated = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );
        ResponseEntity<Map> forbidden = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                Map.class
        );

        // then
        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthenticated.getBody().get("code")).isEqualTo("UNAUTHORIZED");
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbidden.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    private String createManagerAndLogin(String name, String email) {
        memberRepository.save(new Member(name, email, "password", Role.MANAGER));
        return login(email);
    }

    private long createStoreTheme(String storeName, String themeName, String managerEmail) {
        Member manager = memberRepository.findByEmail(managerEmail).orElseThrow();
        Store store = storeRepository.save(new Store(storeName, manager));
        Theme theme = new Theme(themeName, "방탈출", "https://example.com/" + themeName + ".jpg");
        theme.assignStore(store);
        return themeRepository.save(theme).getId();
    }

    private String createMemberAndLogin(String name, String email) {
        createMember(name, email);
        return login(email);
    }

    private ResponseEntity<List> findManagedReservations(String token) {
        return restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                List.class
        );
    }

    private ResponseEntity<Map> changeManagedReservation(
            String token,
            long reservationId,
            LocalDate date,
            long timeId
    ) {
        Map<String, Object> request = Map.of(
                "date", date.toString(),
                "timeId", timeId
        );

        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders(token)),
                Map.class
        );
    }

    private ResponseEntity<Void> cancelManagedReservation(String token, long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }
}

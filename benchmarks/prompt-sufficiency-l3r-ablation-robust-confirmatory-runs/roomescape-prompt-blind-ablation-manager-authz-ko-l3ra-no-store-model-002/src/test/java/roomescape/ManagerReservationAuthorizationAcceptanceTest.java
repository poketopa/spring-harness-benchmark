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
import org.springframework.core.ParameterizedTypeReference;
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
class ManagerReservationAuthorizationAcceptanceTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 조회, 변경, 삭제할 수 있다")
    void managerCanAccessOwnStoreReservation() {
        // given
        Member manager = createManager("매니저", "manager@example.com");
        String managerToken = login(manager.getEmail());
        long memberId = createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long themeId = createStoreTheme("강남점", manager, "어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        long reservationId = createReservationId(memberToken, LocalDate.of(2030, 5, 1), tenOClockId, themeId);

        // when
        ResponseEntity<Map> found = findManagedReservation(managerToken, reservationId);
        ResponseEntity<Map> changed = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );
        ResponseEntity<Void> deleted = cancelManagedReservation(managerToken, reservationId);

        // then
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody().get("memberId")).isEqualTo((int) memberId);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) elevenOClockId);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(memberToken)).isEmpty();
    }

    @Test
    @DisplayName("매니저 예약 목록에는 자신이 관리하는 매장의 예약만 포함된다")
    void managerFindsOnlyOwnStoreReservations() {
        // given
        Member manager = createManager("매니저", "manager@example.com");
        Member otherManager = createManager("다른매니저", "other-manager@example.com");
        String managerToken = login(manager.getEmail());
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long ownThemeId = createStoreTheme("강남점", manager, "어둠의 방");
        long otherThemeId = createStoreTheme("홍대점", otherManager, "빛의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        long ownReservationId = createReservationId(brownToken, LocalDate.of(2030, 5, 1), tenOClockId, ownThemeId);
        long otherReservationId = createReservationId(conyToken, LocalDate.of(2030, 5, 1), elevenOClockId, otherThemeId);

        // when
        ResponseEntity<List<Map<String, Object>>> response = findManagedReservations(managerToken);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().stream()
                .map(reservation -> reservation.get("id"))
                .map(id -> ((Number) id).longValue())
                .toList())
                .containsExactly(ownReservationId)
                .doesNotContain(otherReservationId);
    }

    @Test
    @DisplayName("매니저는 다른 매장의 예약에 접근할 수 없다")
    void managerCannotAccessOtherStoreReservation() {
        // given
        Member manager = createManager("매니저", "manager@example.com");
        Member otherManager = createManager("다른매니저", "other-manager@example.com");
        String managerToken = login(manager.getEmail());
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long themeId = createStoreTheme("홍대점", otherManager, "빛의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        long reservationId = createReservationId(memberToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        // when
        ResponseEntity<Map> found = findManagedReservation(managerToken, reservationId);
        ResponseEntity<Map> changed = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                timeId
        );
        ResponseEntity<Map> deleted = cancelManagedReservationAsMap(managerToken, reservationId);

        // then
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(found.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(changed.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(deleted.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("매니저가 아닌 로그인 회원은 관리자 예약에 접근할 수 없다")
    void nonManagerCannotAccessManagerReservation() {
        // given
        Member manager = createManager("매니저", "manager@example.com");
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createStoreTheme("강남점", manager, "어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        long reservationId = createReservationId(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        // when
        ResponseEntity<Map> response = findManagedReservation(conyToken, reservationId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 사용자는 관리자 예약에 접근할 수 없다")
    void unauthenticatedCannotAccessManagerReservation() {
        // when
        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations/1",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    private Member createManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }

    private long createStoreTheme(String storeName, Member manager, String themeName) {
        Store store = storeRepository.save(new Store(storeName, manager));
        Theme theme = themeRepository.save(new Theme(themeName, "방탈출", "https://example.com/" + themeName + ".jpg", store));
        return theme.getId();
    }

    private long createReservationId(String token, LocalDate date, long timeId, long themeId) {
        return ((Number) createReservation(token, date, timeId, themeId)
                .getBody()
                .get("id")).longValue();
    }

    private ResponseEntity<List<Map<String, Object>>> findManagedReservations(String token) {
        return restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                new ParameterizedTypeReference<>() {
                }
        );
    }

    private ResponseEntity<Map> findManagedReservation(String token, long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }

    private ResponseEntity<Map> changeManagedReservation(String token, long reservationId, LocalDate date, long timeId) {
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

    private ResponseEntity<Map> cancelManagedReservationAsMap(String token, long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }
}

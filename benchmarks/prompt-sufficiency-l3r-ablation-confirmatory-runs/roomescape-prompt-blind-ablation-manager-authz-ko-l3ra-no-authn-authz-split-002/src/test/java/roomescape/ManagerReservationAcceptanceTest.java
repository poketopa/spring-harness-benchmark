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
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 조회하고 변경하고 삭제할 수 있다")
    void managerFindsChangesAndCancelsOwnStoreReservation() {
        // given
        Member manager = createManager("매니저", "manager@example.com");
        String managerToken = login("manager@example.com");
        long themeId = createManagedTheme("강남점", manager, "어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        createMember("브라운", "brown@example.com");
        String userToken = login("brown@example.com");
        LocalDate date = LocalDate.of(2030, 5, 1);
        long reservationId = ((Number) createReservation(userToken, date, tenOClockId, themeId)
                .getBody()
                .get("id")).longValue();

        // when
        ResponseEntity<List> listResponse = findManagedReservations(managerToken);
        ResponseEntity<Map> changeResponse = changeManagedReservation(
                managerToken,
                reservationId,
                date.plusDays(1),
                elevenOClockId
        );
        ResponseEntity<Void> cancelResponse = cancelManagedReservation(managerToken, reservationId, Void.class);

        // then
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).hasSize(1);
        assertThat(((Map<String, Object>) listResponse.getBody().getFirst()).get("id")).isEqualTo((int) reservationId);
        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changeResponse.getBody().get("date")).isEqualTo(date.plusDays(1).toString());
        assertThat(changeResponse.getBody().get("timeId")).isEqualTo((int) elevenOClockId);
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findManagedReservations(managerToken).getBody()).isEmpty();
    }

    @Test
    @DisplayName("매니저는 다른 매장의 예약을 삭제할 수 없다")
    void managerCannotCancelOtherStoreReservation() {
        // given
        Member ownManager = createManager("강남 매니저", "gangnam-manager@example.com");
        Member otherManager = createManager("잠실 매니저", "jamsil-manager@example.com");
        String ownManagerToken = login("gangnam-manager@example.com");
        String otherManagerToken = login("jamsil-manager@example.com");
        createManagedTheme("강남점", ownManager, "강남 방");
        long otherThemeId = createManagedTheme("잠실점", otherManager, "잠실 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createMember("브라운", "brown@example.com");
        String userToken = login("brown@example.com");
        long reservationId = ((Number) createReservation(
                userToken,
                LocalDate.of(2030, 5, 1),
                timeId,
                otherThemeId
        ).getBody().get("id")).longValue();

        // when
        ResponseEntity<Map> rejected = cancelManagedReservation(ownManagerToken, reservationId, Map.class);

        // then
        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(rejected.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(findManagedReservations(otherManagerToken).getBody()).hasSize(1);
    }

    @Test
    @DisplayName("매니저가 아닌 사용자는 매장 예약을 조회할 수 없다")
    void nonManagerCannotFindManagedReservations() {
        // given
        createMember("브라운", "brown@example.com");
        String userToken = login("brown@example.com");

        // when
        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    private Member createManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }

    private long createManagedTheme(String storeName, Member manager, String themeName) {
        Store store = storeRepository.save(new Store(storeName, manager));
        Theme theme = themeRepository.save(new Theme(
                themeName,
                "방탈출",
                "https://example.com/" + themeName + ".jpg",
                store
        ));
        return theme.getId();
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

    private <T> ResponseEntity<T> cancelManagedReservation(String token, long reservationId, Class<T> responseType) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                responseType
        );
    }
}

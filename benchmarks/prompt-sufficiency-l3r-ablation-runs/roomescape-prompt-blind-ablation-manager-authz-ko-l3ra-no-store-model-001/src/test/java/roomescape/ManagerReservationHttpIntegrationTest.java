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
import roomescape.domain.ManagerStore;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.repository.ManagerStoreRepository;
import roomescape.repository.MemberRepository;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationHttpIntegrationTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ManagerStoreRepository managerStoreRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 조회하고 변경하고 삭제할 수 있다")
    void managerFindsChangesAndCancelsOwnStoreReservation() {
        Store store = storeRepository.save(new Store("강남점"));
        String managerToken = createManagerToken("매니저", "manager@example.com", store);
        long themeId = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg", store))
                .getId();
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long reservationId = ((Number) createReservation(
                memberToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<Object> found = findStoreReservations(managerToken, store.getId());
        ResponseEntity<Map> changed = changeManagerReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );
        ResponseEntity<Void> cancelled = cancelManagerReservation(managerToken, reservationId);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<?> reservations = (List<?>) found.getBody();
        assertThat(reservations).hasSize(1);
        assertThat(((Map<?, ?>) reservations.getFirst()).get("id")).isEqualTo((int) reservationId);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) elevenOClockId);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(memberToken)).isEmpty();
    }

    @Test
    @DisplayName("매니저는 자신이 관리하지 않는 매장의 예약 조회와 변경과 삭제를 할 수 없다")
    void managerCannotAccessOtherStoreReservations() {
        Store ownStore = storeRepository.save(new Store("강남점"));
        Store otherStore = storeRepository.save(new Store("잠실점"));
        String managerToken = createManagerToken("매니저", "manager@example.com", ownStore);
        long otherThemeId = themeRepository.save(new Theme(
                "비밀의 방",
                "방탈출",
                "https://example.com/secret.jpg",
                otherStore
        )).getId();
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long reservationId = ((Number) createReservation(
                memberToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                otherThemeId
        ).getBody().get("id")).longValue();

        ResponseEntity<Object> found = findStoreReservations(managerToken, otherStore.getId());
        ResponseEntity<Map> changed = changeManagerReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );
        ResponseEntity<Void> cancelled = cancelManagerReservation(managerToken, reservationId);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((Map<?, ?>) found.getBody()).get("code")).isEqualTo("FORBIDDEN");
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(changed.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("로그인했지만 매니저가 아닌 회원은 매장 예약을 조회할 수 없다")
    void nonManagerCannotFindStoreReservations() {
        Store store = storeRepository.save(new Store("강남점"));
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");

        ResponseEntity<Object> response = findStoreReservations(memberToken, store.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((Map<?, ?>) response.getBody()).get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 사용자는 매장 예약을 조회할 수 없다")
    void unauthenticatedCannotFindStoreReservations() {
        Store store = storeRepository.save(new Store("강남점"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/stores/" + store.getId() + "/reservations",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    private String createManagerToken(String name, String email, Store store) {
        Member manager = memberRepository.save(new Member(name, email, "password", Role.MANAGER));
        managerStoreRepository.save(new ManagerStore(manager, store));
        return login(email);
    }

    private ResponseEntity<Object> findStoreReservations(String token, Long storeId) {
        return restTemplate.exchange(
                "/manager/stores/" + storeId + "/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Object.class
        );
    }

    private ResponseEntity<Map> changeManagerReservation(
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

    private ResponseEntity<Void> cancelManagerReservation(String token, long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }
}

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
import roomescape.domain.ReservationTime;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.repository.ManagerStoreRepository;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationHttpIntegrationTest extends AcceptanceTestSupport {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ManagerStoreRepository managerStoreRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장 예약을 조회, 변경, 삭제할 수 있다")
    void managerFindsChangesAndCancelsOwnStoreReservation() {
        // given
        Store store = createStore("강남점");
        String managerToken = createManagerToken("manager@example.com", store);
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long themeId = createStoreTheme(store, "어둠의 방");
        long tenOClockId = createStoreTime(store, LocalTime.of(10, 0));
        long elevenOClockId = createStoreTime(store, LocalTime.of(11, 0));
        long reservationId = createReservationAndExtractId(
                memberToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        );

        // when
        ResponseEntity<List> reservations = findManagedReservations(managerToken);
        ResponseEntity<Map> reservation = findManagedReservation(managerToken, reservationId);
        ResponseEntity<Map> changed = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );
        ResponseEntity<Void> cancelled = cancelManagedReservation(managerToken, reservationId);

        // then
        assertThat(reservations.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reservations.getBody()).hasSize(1);
        assertThat(reservation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reservation.getBody().get("themeId")).isEqualTo((int) themeId);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(((Number) changed.getBody().get("timeId")).longValue()).isEqualTo(elevenOClockId);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findManagedReservations(managerToken).getBody()).isEmpty();
    }

    @Test
    @DisplayName("매니저는 다른 매장 예약을 조회할 수 없다")
    void managerCannotFindOtherStoreReservation() {
        Store ownStore = createStore("강남점");
        Store otherStore = createStore("잠실점");
        String managerToken = createManagerToken("manager@example.com", ownStore);
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long otherThemeId = createStoreTheme(otherStore, "다른 방");
        long otherTimeId = createStoreTime(otherStore, LocalTime.of(10, 0));
        long reservationId = createReservationAndExtractId(
                memberToken,
                LocalDate.of(2030, 5, 1),
                otherTimeId,
                otherThemeId
        );

        ResponseEntity<Map> response = findManagedReservation(managerToken, reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("매니저가 아닌 로그인 회원은 매니저 예약 조회 권한이 없다")
    void nonManagerCannotFindManagedReservations() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");

        ResponseEntity<Map> response = findManagedReservation(token, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 요청은 매니저 예약 조회 인증에 실패한다")
    void unauthenticatedCannotFindManagedReservations() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations/1",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    private Store createStore(String name) {
        return storeRepository.save(new Store(name));
    }

    private String createManagerToken(String email, Store store) {
        Member manager = memberRepository.save(new Member("매니저", email, "password", Role.MANAGER));
        managerStoreRepository.save(new ManagerStore(manager, store));
        return login(email);
    }

    private long createStoreTheme(Store store, String name) {
        Theme theme = themeRepository.save(new Theme(store, name, "방탈출", "https://example.com/" + name + ".jpg"));
        return theme.getId();
    }

    private long createStoreTime(Store store, LocalTime startAt) {
        ReservationTime time = timeRepository.save(new ReservationTime(store, startAt));
        return time.getId();
    }

    private long createReservationAndExtractId(String token, LocalDate date, long timeId, long themeId) {
        ResponseEntity<Map> response = createReservation(token, date, timeId, themeId);
        assertThat(response.getStatusCode())
                .as("reservation creation body: %s", response.getBody())
                .isEqualTo(HttpStatus.CREATED);
        return ((Number) response.getBody().get("id")).longValue();
    }

    private ResponseEntity<List> findManagedReservations(String token) {
        return restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                List.class
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

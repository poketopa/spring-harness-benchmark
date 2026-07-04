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
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.repository.ManagerStoreRepository;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
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
    private MemberRepository memberRepository;

    @Autowired
    private ManagerStoreRepository managerStoreRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 조회, 변경, 삭제할 수 있다")
    void managerCanFindChangeAndCancelOwnStoreReservation() {
        Store store = saveStore("강남점");
        Member manager = saveManager("매니저", "manager@example.com", store);
        Member customer = saveUser("브라운", "brown@example.com");
        Theme theme = saveTheme(store, "어둠의 방");
        ReservationTime tenOClock = saveTime(store, LocalTime.of(10, 0));
        ReservationTime elevenOClock = saveTime(store, LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(customer, theme, tenOClock, date));
        String managerToken = login(manager.getEmail());

        ResponseEntity<List> findResponse = findManagedStoreReservations(managerToken, store.getId());
        ResponseEntity<Map> changeResponse = managerChangeReservation(
                managerToken,
                reservation.getId(),
                LocalDate.of(2030, 5, 2),
                elevenOClock.getId()
        );
        ResponseEntity<Void> cancelResponse = managerCancelReservation(managerToken, reservation.getId());

        assertThat(findResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findResponse.getBody()).hasSize(1);
        assertThat(((Map<?, ?>) findResponse.getBody().getFirst()).get("storeId")).isEqualTo(store.getId().intValue());

        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changeResponse.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(changeResponse.getBody().get("timeId")).isEqualTo(elevenOClock.getId().intValue());

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("매니저는 다른 매장의 예약에 접근할 수 없다")
    void managerCannotAccessOtherStoreReservation() {
        Store ownStore = saveStore("강남점");
        Store otherStore = saveStore("홍대점");
        Member manager = saveManager("매니저", "manager@example.com", ownStore);
        Member customer = saveUser("브라운", "brown@example.com");
        Reservation otherReservation = saveReservation(otherStore, customer);
        String managerToken = login(manager.getEmail());

        ResponseEntity<Map> response = managerFindReservation(managerToken, otherReservation.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("매니저가 아닌 로그인 회원은 매니저 예약 API에 접근할 수 없다")
    void nonManagerCannotAccessManagerReservationApi() {
        Store store = saveStore("강남점");
        Member user = saveUser("브라운", "brown@example.com");
        Reservation reservation = saveReservation(store, user);
        String userToken = login(user.getEmail());

        ResponseEntity<Map> response = managerFindReservation(userToken, reservation.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 사용자는 매니저 예약 API 인증에 실패한다")
    void unauthenticatedCannotAccessManagerReservationApi() {
        Store store = saveStore("강남점");
        Member customer = saveUser("브라운", "brown@example.com");
        Reservation reservation = saveReservation(store, customer);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations/" + reservation.getId(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    private Store saveStore(String name) {
        return storeRepository.save(new Store(name));
    }

    private Member saveManager(String name, String email, Store store) {
        Member manager = memberRepository.save(new Member(name, email, "password", Role.MANAGER));
        managerStoreRepository.save(new ManagerStore(manager, store));
        return manager;
    }

    private Member saveUser(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private Theme saveTheme(Store store, String name) {
        return themeRepository.save(new Theme(name, "방탈출", "https://example.com/" + name + ".jpg", store));
    }

    private ReservationTime saveTime(Store store, LocalTime startAt) {
        return timeRepository.save(new ReservationTime(startAt, store));
    }

    private Reservation saveReservation(Store store, Member customer) {
        Theme theme = saveTheme(store, "어둠의 방");
        ReservationTime time = saveTime(store, LocalTime.of(10, 0));
        return reservationRepository.save(new Reservation(customer, theme, time, LocalDate.of(2030, 5, 1)));
    }

    private ResponseEntity<List> findManagedStoreReservations(String token, long storeId) {
        return restTemplate.exchange(
                "/manager/stores/" + storeId + "/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                List.class
        );
    }

    private ResponseEntity<Map> managerFindReservation(String token, long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }

    private ResponseEntity<Map> managerChangeReservation(
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

    private ResponseEntity<Void> managerCancelReservation(String token, long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }
}

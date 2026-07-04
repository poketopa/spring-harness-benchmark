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
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.domain.StoreManager;
import roomescape.domain.Theme;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationAuthorizationHttpIntegrationTest extends AcceptanceTestSupport {

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 조회, 변경, 삭제할 수 있다")
    void managerCanFindChangeAndCancelOwnStoreReservation() {
        Store store = store("강남점");
        String managerToken = managerToken("manager@example.com", store);
        Reservation reservation = reservation(store, "어둠의 방", LocalTime.of(10, 0), "brown@example.com");
        ReservationTime changedTime = timeRepository.save(new ReservationTime(store, LocalTime.of(11, 0)));
        LocalDate changedDate = LocalDate.of(2030, 5, 2);

        ResponseEntity<Map> found = findReservation(managerToken, reservation.getId());
        ResponseEntity<List> storeReservations = findStoreReservations(managerToken, store.getId());
        ResponseEntity<Map> changed = changeReservation(managerToken, reservation.getId(), changedDate, changedTime.getId());
        ResponseEntity<Void> canceled = cancelReservation(managerToken, reservation.getId());

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody().get("id")).isEqualTo(reservation.getId().intValue());
        assertThat(storeReservations.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(storeReservations.getBody()).hasSize(1);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo(changedDate.toString());
        assertThat(changed.getBody().get("timeId")).isEqualTo(changedTime.getId().intValue());
        assertThat(canceled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("매니저가 다른 매장의 예약을 조회하면 인가 실패 응답을 반환한다")
    void managerCannotFindOtherStoreReservation() {
        Store managedStore = store("강남점");
        Store otherStore = store("잠실점");
        String managerToken = managerToken("manager@example.com", managedStore);
        Reservation otherStoreReservation = reservation(
                otherStore,
                "비밀의 방",
                LocalTime.of(10, 0),
                "brown@example.com"
        );

        ResponseEntity<Map> response = findReservation(managerToken, otherStoreReservation.getId());
        ResponseEntity<Map> storeResponse = restTemplate.exchange(
                "/stores/" + otherStore.getId() + "/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(managerToken)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(storeResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(storeResponse.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("매니저가 아닌 회원이 예약을 조회하면 인가 실패 응답을 반환한다")
    void nonManagerCannotFindReservation() {
        Store store = store("강남점");
        memberRepository.save(new Member("코니", "cony@example.com", "password"));
        String userToken = login("cony@example.com");
        Reservation reservation = reservation(store, "어둠의 방", LocalTime.of(10, 0), "brown@example.com");

        ResponseEntity<Map> response = findReservation(userToken, reservation.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 예약 조회는 인증 실패 응답을 반환한다")
    void unauthenticatedFindReservationReturnsUnauthorized() {
        Store store = store("강남점");
        Reservation reservation = reservation(store, "어둠의 방", LocalTime.of(10, 0), "brown@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/reservations/" + reservation.getId(),
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    private Store store(String name) {
        return storeRepository.save(new Store(name));
    }

    private String managerToken(String email, Store store) {
        Member manager = memberRepository.save(new Member("매니저", email, "password", Role.MANAGER));
        storeManagerRepository.save(new StoreManager(manager, store));
        return login(email);
    }

    private Reservation reservation(Store store, String themeName, LocalTime startAt, String memberEmail) {
        Member member = memberRepository.save(new Member("예약자", memberEmail, "password"));
        Theme theme = themeRepository.save(new Theme(
                store,
                themeName,
                "방탈출",
                "https://example.com/" + themeName + ".jpg"
        ));
        ReservationTime time = timeRepository.save(new ReservationTime(store, startAt));
        return reservationRepository.save(new Reservation(member, theme, time, LocalDate.of(2030, 5, 1)));
    }
}

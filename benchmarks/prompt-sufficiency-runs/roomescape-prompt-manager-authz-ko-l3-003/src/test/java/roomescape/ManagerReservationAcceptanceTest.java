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
import roomescape.domain.Theme;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationAcceptanceTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationTimeRepository reservationTimeRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 조회, 변경, 삭제할 수 있다")
    void managerCanViewChangeAndDeleteOwnStoreReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member customer = saveUser("브라운", "brown@example.com");
        Store store = storeRepository.save(new Store("잠실점", manager));
        Theme theme = themeRepository.save(new Theme(store, "어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime tenOClock = reservationTimeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        ReservationTime elevenOClock = reservationTimeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        Reservation reservation = reservationRepository.save(new Reservation(
                customer,
                theme,
                tenOClock,
                LocalDate.of(2030, 5, 1)
        ));
        String token = login("manager@example.com");

        ResponseEntity<List> reservations = findManagerReservations(token);
        ResponseEntity<Map> changed = changeManagerReservation(
                token,
                reservation.getId(),
                LocalDate.of(2030, 5, 2),
                elevenOClock.getId()
        );
        ResponseEntity<Void> canceled = cancelManagerReservation(token, reservation.getId());

        assertThat(reservations.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(reservations.getBody()).hasSize(1);
        assertThat(((Map<?, ?>) reservations.getBody().getFirst()).get("themeName")).isEqualTo("어둠의 방");
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(((Number) changed.getBody().get("timeId")).longValue()).isEqualTo(elevenOClock.getId());
        assertThat(canceled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("매니저가 다른 매장의 예약에 접근하면 인가 실패 응답을 반환한다")
    void managerCannotAccessOtherStoreReservation() {
        Member manager = saveManager("잠실 매니저", "manager@example.com");
        Member otherManager = saveManager("강남 매니저", "other-manager@example.com");
        Member customer = saveUser("브라운", "brown@example.com");
        storeRepository.save(new Store("잠실점", manager));
        Store otherStore = storeRepository.save(new Store("강남점", otherManager));
        Theme otherTheme = themeRepository.save(new Theme(
                otherStore,
                "다른 방",
                "방탈출",
                "https://example.com/other.jpg"
        ));
        ReservationTime time = reservationTimeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        Reservation reservation = reservationRepository.save(new Reservation(
                customer,
                otherTheme,
                time,
                LocalDate.of(2030, 5, 1)
        ));
        String token = login("manager@example.com");

        ResponseEntity<Map> response = cancelManagerReservationWithBody(token, reservation.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(reservationRepository.findById(reservation.getId())).isPresent();
    }

    @Test
    @DisplayName("일반 회원이 매니저 예약 API에 접근하면 인가 실패 응답을 반환한다")
    void memberCannotAccessManagerReservations() {
        saveUser("브라운", "brown@example.com");
        String token = login("brown@example.com");

        ResponseEntity<Map> response = findManagerReservationsWithBody(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("인증 없이 매니저 예약 API에 접근하면 인증 실패 응답을 반환한다")
    void unauthenticatedRequestCannotAccessManagerReservations() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    private Member saveManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }

    private Member saveUser(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private ResponseEntity<List> findManagerReservations(String token) {
        return restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                List.class
        );
    }

    private ResponseEntity<Map> findManagerReservationsWithBody(String token) {
        return restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }

    private ResponseEntity<Map> changeManagerReservation(
            String token,
            Long reservationId,
            LocalDate date,
            Long timeId
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

    private ResponseEntity<Void> cancelManagerReservation(String token, Long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }

    private ResponseEntity<Map> cancelManagerReservationWithBody(String token, Long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }
}

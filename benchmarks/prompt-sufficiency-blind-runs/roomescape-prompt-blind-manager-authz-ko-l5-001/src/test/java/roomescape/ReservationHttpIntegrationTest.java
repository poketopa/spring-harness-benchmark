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
class ReservationHttpIntegrationTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Test
    @DisplayName("다른 회원의 예약을 변경하면 찾을 수 없음 응답을 반환한다")
    void changeOtherMemberReservationReturnsNotFound() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        long reservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<Map> response = changeReservation(
                conyToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("RESERVATION_NOT_FOUND");
    }

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약만 조회하고 변경하고 삭제할 수 있다")
    void managerCanManageOwnStoreReservations() {
        Store ownStore = storeRepository.save(new Store("강남점"));
        Store otherStore = storeRepository.save(new Store("홍대점"));
        Member manager = memberRepository.save(new Member("매니저", "manager@example.com", "password", Role.MANAGER, ownStore));
        Member user = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Theme ownTheme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg", ownStore));
        Theme otherTheme = themeRepository.save(new Theme("빛의 방", "방탈출", "https://example.com/light.jpg", otherStore));
        ReservationTime tenOClock = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        ReservationTime elevenOClock = timeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation ownReservation = reservationRepository.save(new Reservation(user, ownTheme, tenOClock, date));
        reservationRepository.save(new Reservation(user, otherTheme, elevenOClock, date));
        String managerToken = login(manager.getEmail());

        ResponseEntity<List> findResponse = findManagedReservations(managerToken, ownStore.getId());
        ResponseEntity<Map> changeResponse = changeManagedReservation(
                managerToken,
                ownStore.getId(),
                ownReservation.getId(),
                LocalDate.of(2030, 5, 2),
                elevenOClock.getId()
        );
        ResponseEntity<Void> cancelResponse = cancelManagedReservation(
                managerToken,
                ownStore.getId(),
                ownReservation.getId()
        );

        assertThat(findResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findResponse.getBody()).hasSize(1);
        Map<String, Object> foundReservation = (Map<String, Object>) findResponse.getBody().getFirst();
        assertThat(((Number) foundReservation.get("id")).longValue()).isEqualTo(ownReservation.getId());
        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changeResponse.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(reservationRepository.findById(ownReservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("매니저가 다른 매장 예약에 접근하면 인가 실패 응답을 반환한다")
    void managerCannotAccessOtherStoreReservations() {
        Store ownStore = storeRepository.save(new Store("강남점"));
        Store otherStore = storeRepository.save(new Store("홍대점"));
        Member manager = memberRepository.save(new Member("매니저", "manager@example.com", "password", Role.MANAGER, ownStore));
        String managerToken = login(manager.getEmail());

        ResponseEntity<Map> response = findManagedReservationsError(managerToken, otherStore.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("일반 회원이 매니저 예약 API에 접근하면 인가 실패 응답을 반환한다")
    void userCannotAccessManagerReservations() {
        Store store = storeRepository.save(new Store("강남점"));
        createMember("브라운", "brown@example.com");
        String userToken = login("brown@example.com");

        ResponseEntity<Map> response = findManagedReservationsError(userToken, store.getId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 요청이 매니저 예약 API에 접근하면 인증 실패 응답을 반환한다")
    void unauthenticatedCannotAccessManagerReservations() {
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

    private ResponseEntity<List> findManagedReservations(String token, Long storeId) {
        return restTemplate.exchange(
                "/manager/stores/" + storeId + "/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                List.class
        );
    }

    private ResponseEntity<Map> findManagedReservationsError(String token, Long storeId) {
        return restTemplate.exchange(
                "/manager/stores/" + storeId + "/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }

    private ResponseEntity<Map> changeManagedReservation(
            String token,
            Long storeId,
            Long reservationId,
            LocalDate date,
            Long timeId
    ) {
        Map<String, Object> request = Map.of(
                "date", date.toString(),
                "timeId", timeId
        );

        return restTemplate.exchange(
                "/manager/stores/" + storeId + "/reservations/" + reservationId,
                HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders(token)),
                Map.class
        );
    }

    private ResponseEntity<Void> cancelManagedReservation(String token, Long storeId, Long reservationId) {
        return restTemplate.exchange(
                "/manager/stores/" + storeId + "/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }
}

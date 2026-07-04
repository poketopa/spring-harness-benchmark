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
import org.springframework.core.ParameterizedTypeReference;
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
class ManagerReservationHttpIntegrationTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 조회, 변경, 삭제할 수 있다")
    void managerCanManageOwnStoreReservations() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member member = saveUser("브라운", "brown@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime tenOClock = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        ReservationTime elevenOClock = timeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        storeRepository.save(new Store(manager, theme));
        Reservation reservation = reservationRepository.save(new Reservation(
                member,
                theme,
                tenOClock,
                LocalDate.of(2030, 5, 1)
        ));
        String managerToken = login("manager@example.com");

        ResponseEntity<List<Map<String, Object>>> findResponse = findManagedReservations(managerToken);
        ResponseEntity<Map> changeResponse = changeManagedReservation(
                managerToken,
                reservation.getId(),
                LocalDate.of(2030, 5, 2),
                elevenOClock.getId()
        );
        ResponseEntity<Void> cancelResponse = cancelManagedReservation(managerToken, reservation.getId());

        assertThat(findResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findResponse.getBody()).hasSize(1);
        assertThat(findResponse.getBody().getFirst().get("id"))
                .isEqualTo(reservation.getId().intValue());
        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changeResponse.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(changeResponse.getBody().get("timeId")).isEqualTo(elevenOClock.getId().intValue());
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("매니저가 다른 매장 예약에 접근하면 인가 실패 응답을 반환한다")
    void managerCannotManageOtherStoreReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member otherManager = saveManager("다른매니저", "other-manager@example.com");
        Member member = saveUser("브라운", "brown@example.com");
        Theme ownTheme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        Theme otherTheme = themeRepository.save(new Theme("빛의 방", "방탈출", "https://example.com/light.jpg"));
        ReservationTime tenOClock = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        ReservationTime elevenOClock = timeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        storeRepository.save(new Store(manager, ownTheme));
        storeRepository.save(new Store(otherManager, otherTheme));
        Reservation otherReservation = reservationRepository.save(new Reservation(
                member,
                otherTheme,
                tenOClock,
                LocalDate.of(2030, 5, 1)
        ));
        String managerToken = login("manager@example.com");

        ResponseEntity<Map> changeResponse = changeManagedReservation(
                managerToken,
                otherReservation.getId(),
                LocalDate.of(2030, 5, 2),
                elevenOClock.getId()
        );
        ResponseEntity<Map> cancelResponse = cancelManagedReservationWithBody(managerToken, otherReservation.getId());

        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(changeResponse.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(cancelResponse.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("일반 회원이 매니저 예약 목록에 접근하면 인가 실패 응답을 반환한다")
    void userCannotFindManagedReservations() {
        saveUser("브라운", "brown@example.com");
        String userToken = login("brown@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 사용자가 매니저 예약 목록에 접근하면 인증 실패 응답을 반환한다")
    void unauthenticatedUserCannotFindManagedReservations() {
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
        return memberRepository.save(new Member(name, email, "password", Role.USER));
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

    private ResponseEntity<Map> changeManagedReservation(
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

    private ResponseEntity<Void> cancelManagedReservation(String token, Long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }

    private ResponseEntity<Map> cancelManagedReservationWithBody(String token, Long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }
}

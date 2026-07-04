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

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약만 조회한다")
    void managerFindsOnlyOwnStoreReservations() {
        Fixture fixture = saveFixture();
        String managerToken = login("manager@example.com");

        ResponseEntity<List> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(managerToken)),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(item -> ((Number) ((Map<?, ?>) item).get("id")).longValue())
                .containsExactly(fixture.ownReservationId());
    }

    @Test
    @DisplayName("비로그인 사용자는 매니저 예약 조회 시 인증 실패 응답을 받는다")
    void unauthenticatedManagerReservationRequestReturnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/manager/reservations", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("매니저가 아닌 로그인 사용자는 매니저 예약 조회 시 인가 실패 응답을 받는다")
    void nonManagerRequestReturnsForbidden() {
        createMember("브라운", "brown@example.com");
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
    @DisplayName("매니저는 자신이 관리하지 않는 매장의 예약을 변경할 수 없다")
    void managerCannotChangeOtherStoreReservation() {
        Fixture fixture = saveFixture();
        String managerToken = login("manager@example.com");
        Map<String, Object> request = Map.of(
                "date", LocalDate.of(2030, 5, 2).toString(),
                "timeId", fixture.otherTimeId()
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations/" + fixture.otherReservationId(),
                HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders(managerToken)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("매니저는 자신이 관리하지 않는 매장의 예약을 삭제할 수 없다")
    void managerCannotCancelOtherStoreReservation() {
        Fixture fixture = saveFixture();
        String managerToken = login("manager@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations/" + fixture.otherReservationId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(managerToken)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(reservationRepository.findById(fixture.otherReservationId())).isPresent();
    }

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 변경하고 삭제할 수 있다")
    void managerChangesAndCancelsOwnStoreReservation() {
        Fixture fixture = saveFixture();
        String managerToken = login("manager@example.com");
        Map<String, Object> request = Map.of(
                "date", LocalDate.of(2030, 5, 2).toString(),
                "timeId", fixture.otherTimeId()
        );

        ResponseEntity<Map> change = restTemplate.exchange(
                "/manager/reservations/" + fixture.ownReservationId(),
                HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders(managerToken)),
                Map.class
        );
        ResponseEntity<Void> cancel = restTemplate.exchange(
                "/manager/reservations/" + fixture.ownReservationId(),
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(managerToken)),
                Void.class
        );

        assertThat(change.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(change.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(cancel.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(reservationRepository.findById(fixture.ownReservationId())).isEmpty();
    }

    private Fixture saveFixture() {
        Member manager = memberRepository.save(new Member("매니저", "manager@example.com", "password", Role.MANAGER));
        Member otherManager = memberRepository.save(
                new Member("다른 매니저", "other-manager@example.com", "password", Role.MANAGER)
        );
        Member guest = memberRepository.save(new Member("브라운", "brown@example.com", "password"));

        Store ownStore = new Store("강남점", manager);
        Store otherStore = new Store("잠실점", otherManager);
        Theme ownTheme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg", ownStore));
        Theme otherTheme = themeRepository.save(
                new Theme("빛의 방", "방탈출", "https://example.com/light.jpg", otherStore)
        );
        ReservationTime tenOClock = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        ReservationTime elevenOClock = timeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);

        Reservation ownReservation = reservationRepository.save(
                new Reservation(guest, ownTheme, tenOClock, date)
        );
        Reservation otherReservation = reservationRepository.save(
                new Reservation(guest, otherTheme, elevenOClock, date)
        );

        return new Fixture(ownReservation.getId(), otherReservation.getId(), elevenOClock.getId());
    }

    private record Fixture(long ownReservationId, long otherReservationId, long otherTimeId) {
    }
}

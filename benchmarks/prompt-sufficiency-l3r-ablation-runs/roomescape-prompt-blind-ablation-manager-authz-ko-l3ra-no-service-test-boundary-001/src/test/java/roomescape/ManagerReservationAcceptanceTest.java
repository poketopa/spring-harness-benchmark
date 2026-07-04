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
@SuppressWarnings("unchecked")
class ManagerReservationAcceptanceTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약만 조회할 수 있다")
    void managerFindsOnlyManagedStoreReservations() {
        Member manager = createManager("매니저", "manager@example.com");
        Store managedStore = createStore("강남점", manager);
        Store otherStore = createStore("잠실점", createManager("다른 매니저", "other-manager@example.com"));
        long managedThemeId = createTheme("어둠의 방", managedStore);
        long otherThemeId = createTheme("빛의 방", otherStore);
        long timeId = createTime(LocalTime.of(10, 0));
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createReservation(login("brown@example.com"), LocalDate.of(2030, 5, 1), timeId, managedThemeId);
        createReservation(login("cony@example.com"), LocalDate.of(2030, 5, 1), timeId, otherThemeId);

        ResponseEntity<List> response = findManagedReservations(login(manager.getEmail()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        Map<String, Object> reservation = (Map<String, Object>) response.getBody().getFirst();
        assertThat(reservation.get("themeName")).isEqualTo("어둠의 방");
    }

    @Test
    @DisplayName("비로그인 사용자가 매니저 예약을 조회하면 인증 실패 응답을 반환한다")
    void anonymousManagerReservationAccessReturnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("매니저가 아닌 회원이 매니저 예약에 접근하면 인가 실패 응답을 반환한다")
    void nonManagerReservationAccessReturnsForbidden() {
        createMember("브라운", "brown@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(login("brown@example.com"))),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("매니저가 다른 매장의 예약을 조회하거나 변경하거나 삭제하면 인가 실패 응답을 반환한다")
    void managerAccessingOtherStoreReservationReturnsForbidden() {
        Member manager = createManager("매니저", "manager@example.com");
        Member otherManager = createManager("다른 매니저", "other-manager@example.com");
        Store otherStore = createStore("잠실점", otherManager);
        long themeId = createTheme("빛의 방", otherStore);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        createMember("브라운", "brown@example.com");
        long reservationId = ((Number) createReservation(
                login("brown@example.com"),
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        String managerToken = login(manager.getEmail());

        ResponseEntity<Map> found = findManagedReservation(managerToken, reservationId);
        ResponseEntity<Map> changed = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );
        ResponseEntity<Map> canceled = cancelManagedReservationForError(managerToken, reservationId);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(found.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(changed.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(canceled.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(canceled.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 변경하고 삭제할 수 있다")
    void managerChangesAndCancelsManagedStoreReservation() {
        Member manager = createManager("매니저", "manager@example.com");
        Store store = createStore("강남점", manager);
        long themeId = createTheme("어둠의 방", store);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        createMember("브라운", "brown@example.com");
        long reservationId = ((Number) createReservation(
                login("brown@example.com"),
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();
        String managerToken = login(manager.getEmail());

        ResponseEntity<Map> changed = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );
        ResponseEntity<Void> canceled = cancelManagedReservation(managerToken, reservationId);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) elevenOClockId);
        assertThat(canceled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findManagedReservations(managerToken).getBody()).isEmpty();
    }

    private Member createManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }

    private Store createStore(String name, Member manager) {
        return storeRepository.save(new Store(name, manager));
    }

    private long createTheme(String name, Store store) {
        Theme theme = themeRepository.save(new Theme(
                name,
                "방탈출",
                "https://example.com/" + name + ".jpg",
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

    private ResponseEntity<Map> cancelManagedReservationForError(String token, long reservationId) {
        return restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }
}

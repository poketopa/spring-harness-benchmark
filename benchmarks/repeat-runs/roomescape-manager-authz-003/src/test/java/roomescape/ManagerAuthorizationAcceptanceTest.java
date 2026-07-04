package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerAuthorizationAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("매니저는 자기 매장 예약만 조회한다")
    void managerFindsOnlyOwnStoreReservations() {
        long managerId = createManager("매니저", "manager@example.com");
        long otherManagerId = createManager("다른매니저", "other-manager@example.com");
        String managerToken = login("manager@example.com");
        createMember("브라운", "brown@example.com");
        String brownToken = login("brown@example.com");
        long storeId = createStore("강남점", managerId);
        long otherStoreId = createStore("잠실점", otherManagerId);
        long themeId = createTheme("어둠의 방", storeId);
        long otherThemeId = createTheme("밝은 방", otherStoreId);
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);
        createReservation(brownToken, LocalDate.of(2030, 5, 2), timeId, otherThemeId);

        List<Map<String, Object>> reservations = findManagedReservations(managerToken);

        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().get("themeId")).isEqualTo((int) themeId);
    }

    @Test
    @DisplayName("매니저는 자기 매장 예약을 변경하고 삭제할 수 있다")
    void managerChangesAndCancelsOwnStoreReservation() {
        long managerId = createManager("매니저", "manager@example.com");
        String managerToken = login("manager@example.com");
        createMember("브라운", "brown@example.com");
        String brownToken = login("brown@example.com");
        long storeId = createStore("강남점", managerId);
        long themeId = createTheme("어둠의 방", storeId);
        long oldTimeId = createTime(LocalTime.of(10, 0));
        long newTimeId = createTime(LocalTime.of(11, 0));
        long reservationId = ((Number) createReservation(brownToken, LocalDate.of(2030, 5, 1), oldTimeId, themeId)
                .getBody()
                .get("id")).longValue();

        ResponseEntity<Map> changed = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                newTimeId
        );
        ResponseEntity<Void> cancelled = cancelManagedReservation(managerToken, reservationId);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) newTimeId);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("다른 매장 예약에 접근하면 인가 실패를 반환한다")
    void otherStoreReservationAccessReturnsForbidden() {
        long managerId = createManager("매니저", "manager@example.com");
        long otherManagerId = createManager("다른매니저", "other-manager@example.com");
        String managerToken = login("manager@example.com");
        createMember("브라운", "brown@example.com");
        String brownToken = login("brown@example.com");
        long storeId = createStore("강남점", managerId);
        long otherStoreId = createStore("잠실점", otherManagerId);
        createTheme("어둠의 방", storeId);
        long otherThemeId = createTheme("밝은 방", otherStoreId);
        long timeId = createTime(LocalTime.of(10, 0));
        long reservationId = ((Number) createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, otherThemeId)
                .getBody()
                .get("id")).longValue();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(managerToken)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("로그인하지 않은 요청과 권한 없는 요청을 구분한다")
    void distinguishUnauthenticatedAndForbidden() {
        createMember("회원", "user@example.com");
        String userToken = login("user@example.com");

        ResponseEntity<Map> unauthenticated = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );
        ResponseEntity<Map> forbidden = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                Map.class
        );

        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthenticated.getBody().get("code")).isEqualTo("UNAUTHORIZED");
        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbidden.getBody().get("code")).isEqualTo("FORBIDDEN");
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

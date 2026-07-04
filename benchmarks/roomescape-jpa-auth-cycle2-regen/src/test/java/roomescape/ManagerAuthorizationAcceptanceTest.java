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
    @DisplayName("매니저는 자기 매장 예약만 조회하고 변경할 수 있다")
    void managerFindsAndChangesOnlyOwnStoreReservation() {
        long managerId = createManager("매니저", "manager@example.com");
        long otherManagerId = createManager("다른매니저", "other-manager@example.com");
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String managerToken = login("manager@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long storeId = createStore("강남점", managerId);
        long otherStoreId = createStore("잠실점", otherManagerId);
        long themeId = createTheme("어둠의 방", storeId);
        long otherThemeId = createTheme("빛의 방", otherStoreId);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        long reservationId = ((Number) createReservation(brownToken, date, tenOClockId, themeId)
                .getBody()
                .get("id")).longValue();
        long otherReservationId = ((Number) createReservation(conyToken, date, tenOClockId, otherThemeId)
                .getBody()
                .get("id")).longValue();

        ResponseEntity<List> managedReservations = findManagedReservations(managerToken);
        ResponseEntity<Map> changed = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );
        ResponseEntity<Void> cancelled = cancelManagedReservation(managerToken, reservationId);
        ResponseEntity<Map> forbidden = restTemplate.exchange(
                "/manager/reservations/" + otherReservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(managerToken)),
                Map.class
        );

        assertThat(managedReservations.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(managedReservations.getBody()).hasSize(1);
        Map<String, Object> managedReservation = (Map<String, Object>) managedReservations.getBody().getFirst();
        assertThat(((Number) managedReservation.get("id")).longValue()).isEqualTo(reservationId);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) elevenOClockId);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(forbidden.getBody().get("code")).isEqualTo("FORBIDDEN");
    }
}

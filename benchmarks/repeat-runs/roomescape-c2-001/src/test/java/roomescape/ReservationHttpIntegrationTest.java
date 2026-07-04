package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
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
class ReservationHttpIntegrationTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("예약 변경 요청 값이 누락되면 검증 실패 응답을 반환한다")
    void invalidChangeRequestReturnsBadRequest() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        long reservationId = ((Number) createReservation(token, LocalDate.of(2030, 5, 1), timeId, themeId)
                .getBody()
                .get("id")).longValue();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/reservations/" + reservationId,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("date", "2030-05-02"), authHeaders(token)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_INPUT");
    }

    @Test
    @DisplayName("예약이 존재하는 시간은 삭제할 수 없다")
    void deleteReservedTimeReturnsConflict() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(token, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/times/" + timeId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("RESERVATION_TIME_IN_USE");
    }
}

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
class ReservationTimeHttpIntegrationTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("예약이 존재하지 않는 시간은 삭제할 수 있다")
    void deleteUnusedTimeReturnsNoContent() {
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Void> response = deleteTime(timeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("예약이 존재하는 시간을 삭제하면 충돌 응답을 반환한다")
    void deleteReservedTimeReturnsConflict() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(token, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/times/" + timeId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminToken())),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("RESERVATION_TIME_IN_USE");
    }

    @Test
    @DisplayName("테마 예약 시간 조회 날짜 형식이 잘못되면 검증 실패 응답을 반환한다")
    void invalidThemeTimesDateReturnsBadRequest() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/themes/1/times?date=wrong-date",
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_INPUT");
    }
}

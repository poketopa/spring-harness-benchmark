package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationHttpIntegrationTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("매니저가 아닌 회원이 예약을 변경하면 인가 실패 응답을 반환한다")
    void changeReservationByNonManagerReturnsForbidden() {
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

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }
}

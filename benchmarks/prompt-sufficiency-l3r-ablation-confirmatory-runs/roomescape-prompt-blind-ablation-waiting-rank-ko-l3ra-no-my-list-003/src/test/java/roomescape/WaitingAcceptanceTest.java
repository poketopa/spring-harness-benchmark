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
class WaitingAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("예약이 차 있는 슬롯에만 대기를 신청할 수 있고 내 대기 순번을 응답한다")
    void occupiedSlotWaitingReturnsRank() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);

        ResponseEntity<Map> response = createWaiting(conyToken, date, timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("rank")).isEqualTo(1);
        assertThat(response.getBody().get("themeId")).isEqualTo((int) themeId);
        assertThat(response.getBody().get("timeId")).isEqualTo((int) timeId);
    }

    @Test
    @DisplayName("예약 가능한 빈 슬롯에는 대기를 신청할 수 없다")
    void availableSlotWaitingReturnsBadRequest() {
        createMember("코니", "cony@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> response = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("AVAILABLE_SLOT_WAITING");
    }

    @Test
    @DisplayName("본인의 예약에는 대기를 신청할 수 없다")
    void ownReservationWaitingReturnsBadRequest() {
        createMember("브라운", "brown@example.com");
        String brownToken = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);

        ResponseEntity<Map> response = createWaiting(brownToken, date, timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("OWN_RESERVATION_WAITING");
    }

    @Test
    @DisplayName("같은 회원은 같은 슬롯에 중복 대기를 신청할 수 없다")
    void duplicateWaitingReturnsConflict() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);
        createWaiting(conyToken, date, timeId, themeId);

        ResponseEntity<Map> response = createWaiting(conyToken, date, timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("DUPLICATE_WAITING");
    }

    @Test
    @DisplayName("대기 순번은 신청 순서와 식별자 tie-breaker로 결정된다")
    void waitingRankOrdersByCreatedAtAndId() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        createMember("문", "moon@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        String moonToken = login("moon@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);

        ResponseEntity<Map> first = createWaiting(conyToken, date, timeId, themeId);
        ResponseEntity<Map> second = createWaiting(sallyToken, date, timeId, themeId);
        ResponseEntity<Map> third = createWaiting(moonToken, date, timeId, themeId);

        assertThat(first.getBody().get("rank")).isEqualTo(1);
        assertThat(second.getBody().get("rank")).isEqualTo(2);
        assertThat(third.getBody().get("rank")).isEqualTo(3);
    }
}

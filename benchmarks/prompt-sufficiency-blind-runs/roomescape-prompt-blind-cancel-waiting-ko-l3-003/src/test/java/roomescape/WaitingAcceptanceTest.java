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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WaitingAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("회원은 이미 예약된 슬롯에 대기를 신청하고 내 예약 목록에서 순번을 확인할 수 있다")
    void memberCreatesWaitingAndFindsItWithRank() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);

        ResponseEntity<Map> conyWaiting = createWaiting(conyToken, date, timeId, themeId);
        ResponseEntity<Map> sallyWaiting = createWaiting(sallyToken, date, timeId, themeId);

        assertThat(conyWaiting.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(conyWaiting.getHeaders().getLocation()).isNotNull();
        assertThat(conyWaiting.getBody().get("rank")).isEqualTo(1);
        assertThat(sallyWaiting.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sallyWaiting.getBody().get("rank")).isEqualTo(2);

        List<Map<String, Object>> conyMine = findMine(conyToken);
        assertThat(conyMine).hasSize(1);
        assertThat(conyMine.getFirst().get("status")).isEqualTo("WAITING");
        assertThat(conyMine.getFirst().get("rank")).isEqualTo(1);

        List<Map<String, Object>> brownMine = findMine(brownToken);
        assertThat(brownMine).hasSize(1);
        assertThat(brownMine.getFirst().get("status")).isEqualTo("RESERVED");
    }

    @Test
    @DisplayName("같은 회원이 같은 슬롯에 중복 대기하면 충돌 응답을 반환한다")
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

        ResponseEntity<Map> duplicate = createWaiting(conyToken, date, timeId, themeId);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("DUPLICATE_WAITING");
    }

    @Test
    @DisplayName("예약되지 않은 슬롯에 대기하면 잘못된 요청 응답을 반환한다")
    void waitingAvailableSlotReturnsBadRequest() {
        createMember("코니", "cony@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> response = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("WAITING_NOT_ALLOWED");
    }

    @Test
    @DisplayName("회원은 본인의 대기를 취소할 수 있다")
    void memberCancelsOwnWaiting() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);
        long waitingId = ((Number) createWaiting(conyToken, date, timeId, themeId).getBody().get("id")).longValue();

        ResponseEntity<Void> response = cancelWaiting(conyToken, waitingId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(conyToken)).isEmpty();
    }

    @Test
    @DisplayName("예약을 취소하면 가장 앞선 대기자가 예약되고 다음 대기자의 순번이 당겨진다")
    void cancelReservationPromotesFirstWaiting() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        long reservationId = ((Number) createReservation(brownToken, date, timeId, themeId).getBody().get("id"))
                .longValue();
        createWaiting(conyToken, date, timeId, themeId);
        createWaiting(sallyToken, date, timeId, themeId);

        ResponseEntity<Void> response = cancelReservation(brownToken, reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(brownToken)).isEmpty();

        List<Map<String, Object>> conyMine = findMine(conyToken);
        assertThat(conyMine).hasSize(1);
        assertThat(conyMine.getFirst().get("status")).isEqualTo("RESERVED");
        assertThat(conyMine.getFirst().get("rank")).isNull();

        List<Map<String, Object>> sallyMine = findMine(sallyToken);
        assertThat(sallyMine).hasSize(1);
        assertThat(sallyMine.getFirst().get("status")).isEqualTo("WAITING");
        assertThat(sallyMine.getFirst().get("rank")).isEqualTo(1);

        List<Map<String, Object>> themeTimes = findThemeTimes(themeId, date);
        assertThat(themeTimes.getFirst().get("reserved")).isEqualTo(true);
    }
}

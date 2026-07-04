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
    @DisplayName("예약된 슬롯에 대기를 신청하면 현재 순번을 반환한다")
    void createWaitingForOccupiedSlotReturnsRank() {
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
    }

    @Test
    @DisplayName("예약되지 않은 슬롯에는 대기를 신청할 수 없다")
    void createWaitingForAvailableSlotReturnsBadRequest() {
        createMember("코니", "cony@example.com");
        String token = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> response = createWaiting(token, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("WAITING_NOT_ALLOWED");
    }

    @Test
    @DisplayName("본인의 예약에는 대기를 신청할 수 없다")
    void createWaitingForOwnReservationReturnsBadRequest() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(token, date, timeId, themeId);

        ResponseEntity<Map> response = createWaiting(token, date, timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("WAITING_NOT_ALLOWED");
    }

    @Test
    @DisplayName("같은 회원은 같은 슬롯에 중복 대기할 수 없다")
    void createDuplicateWaitingReturnsConflict() {
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
    @DisplayName("대기 순번은 신청 순서대로 계산된다")
    void waitingRankFollowsRequestOrder() {
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

        ResponseEntity<Map> firstWaiting = createWaiting(conyToken, date, timeId, themeId);
        ResponseEntity<Map> secondWaiting = createWaiting(sallyToken, date, timeId, themeId);

        assertThat(firstWaiting.getBody().get("rank")).isEqualTo(1);
        assertThat(secondWaiting.getBody().get("rank")).isEqualTo(2);
    }

    @Test
    @DisplayName("내 예약 목록은 예약과 대기를 함께 보여 주고 대기 순번을 포함한다")
    void findMineIncludesReservationAndWaitingRank() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long reservedTimeId = createTime(LocalTime.of(10, 0));
        long waitingTimeId = createTime(LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(conyToken, date, reservedTimeId, themeId);
        createReservation(brownToken, date, waitingTimeId, themeId);

        createWaiting(brownToken, date, reservedTimeId, themeId);
        List<Map<String, Object>> mine = findMine(brownToken);

        assertThat(mine).hasSize(2);
        assertThat(mine)
                .filteredOn(item -> item.get("status").equals("RESERVED"))
                .first()
                .extracting(item -> item.get("rank"))
                .isNull();
        assertThat(mine)
                .filteredOn(item -> item.get("status").equals("WAITING"))
                .first()
                .satisfies(item -> assertThat(item.get("rank")).isEqualTo(1));
    }
}

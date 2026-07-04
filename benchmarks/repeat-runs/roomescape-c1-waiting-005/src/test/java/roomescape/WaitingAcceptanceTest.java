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
    @DisplayName("회원은 예약된 슬롯에 대기를 신청하고 내 예약 목록에서 순번을 확인할 수 있다")
    void memberCreatesWaitingAndFindsItWithRank() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("어피치", "apeach@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String apeachToken = login("apeach@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);

        ResponseEntity<Map> firstWaiting = createWaiting(conyToken, date, timeId, themeId);
        ResponseEntity<Map> secondWaiting = createWaiting(apeachToken, date, timeId, themeId);

        assertThat(firstWaiting.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(firstWaiting.getHeaders().getLocation()).isNotNull();
        assertThat(firstWaiting.getBody().get("rank")).isEqualTo(1);
        assertThat(secondWaiting.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondWaiting.getBody().get("rank")).isEqualTo(2);

        List<Map<String, Object>> mine = findMine(apeachToken);
        assertThat(mine).hasSize(1);
        assertThat(mine.getFirst().get("status")).isEqualTo("WAITING");
        assertThat(mine.getFirst().get("themeName")).isEqualTo("어둠의 방");
        assertThat(mine.getFirst().get("rank")).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 회원이 같은 슬롯에 중복 대기를 신청하면 충돌 응답을 반환한다")
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
    @DisplayName("회원은 본인의 대기를 취소할 수 있다")
    void memberDeletesOwnWaiting() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);
        ResponseEntity<Map> waiting = createWaiting(conyToken, date, timeId, themeId);
        long waitingId = ((Number) waiting.getBody().get("id")).longValue();

        ResponseEntity<Void> deleted = deleteWaiting(conyToken, waitingId);

        assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(conyToken)).isEmpty();
    }

    @Test
    @DisplayName("예약되지 않은 슬롯에 대기를 신청하면 잘못된 요청 응답을 반환한다")
    void waitingForOpenSlotReturnsBadRequest() {
        createMember("코니", "cony@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> response = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("WAITING_NOT_AVAILABLE");
    }
}

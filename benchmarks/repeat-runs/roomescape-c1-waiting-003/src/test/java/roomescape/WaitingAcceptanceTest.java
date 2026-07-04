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
    @DisplayName("회원은 예약된 슬롯에 대기하고 내 예약 목록에서 순번과 상태를 함께 조회한다")
    void memberCreatesWaitingAndFindsItInMine() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> created = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody().get("rank")).isEqualTo(1);

        List<Map<String, Object>> mine = findMine(conyToken);
        assertThat(mine).hasSize(1);
        assertThat(mine.getFirst().get("status")).isEqualTo("WAITING");
        assertThat(mine.getFirst().get("rank")).isEqualTo(1);
        assertThat(mine.getFirst().get("themeName")).isEqualTo("어둠의 방");
    }

    @Test
    @DisplayName("같은 회원은 같은 슬롯에 중복 대기할 수 없다")
    void duplicateWaitingReturnsConflict() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);
        createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> duplicate = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("DUPLICATE_WAITING");
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
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);
        ResponseEntity<Map> created = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);
        long waitingId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Void> cancelled = cancelWaiting(conyToken, waitingId);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(conyToken)).isEmpty();
    }
}

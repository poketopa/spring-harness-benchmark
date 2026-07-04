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
class ReservationAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("회원은 예약을 생성하고 예약된 테마 시간을 조회할 수 있다")
    void memberCreatesReservationAndThemeTimeBecomesReserved() {
        long memberId = createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        List<Map<String, Object>> before = findThemeTimes(themeId, LocalDate.of(2030, 5, 1));
        assertThat(before.getFirst().get("reserved")).isEqualTo(false);

        ResponseEntity<Map> created = createReservation(token, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).isNotNull();
        assertThat(created.getBody().get("memberId")).isEqualTo((int) memberId);

        List<Map<String, Object>> after = findThemeTimes(themeId, LocalDate.of(2030, 5, 1));
        assertThat(after.getFirst().get("reserved")).isEqualTo(true);

        List<Map<String, Object>> mine = findMine(token);
        assertThat(mine).hasSize(1);
        assertThat(mine.getFirst().get("themeName")).isEqualTo("어둠의 방");
    }

    @Test
    @DisplayName("이미 예약된 슬롯을 예약하면 대기로 등록되고 대기 순번을 조회할 수 있다")
    void duplicateSlotCreatesWaitingWithRank() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> firstWaiting = createReservation(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);
        ResponseEntity<Map> secondWaiting = createReservation(sallyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(firstWaiting.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(firstWaiting.getBody().get("status")).isEqualTo("WAITING");
        assertThat(firstWaiting.getBody().get("rank")).isEqualTo(1);
        assertThat(secondWaiting.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondWaiting.getBody().get("status")).isEqualTo("WAITING");
        assertThat(secondWaiting.getBody().get("rank")).isEqualTo(2);

        List<Map<String, Object>> mine = findMine(sallyToken);
        assertThat(mine).hasSize(1);
        assertThat(mine.getFirst().get("status")).isEqualTo("WAITING");
        assertThat(mine.getFirst().get("rank")).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 회원이 같은 슬롯에 중복으로 대기하면 충돌 응답을 반환한다")
    void duplicateWaitingReturnsConflict() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);
        createReservation(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> duplicate = createReservation(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("DUPLICATE_WAITING");
    }

    @Test
    @DisplayName("지난 날짜와 시간으로 예약하면 잘못된 요청 응답을 반환한다")
    void pastReservationReturnsBadRequest() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> response = createReservation(token, LocalDate.of(2000, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("PAST_RESERVATION");
    }
}

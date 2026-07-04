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
    @DisplayName("이미 예약된 슬롯을 다시 예약하면 충돌 응답을 반환한다")
    void duplicateReservationReturnsConflict() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> duplicate = createReservation(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicate.getBody().get("code")).isEqualTo("DUPLICATE_RESERVATION");
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

    @Test
    @DisplayName("회원이 예약을 취소하면 가장 빠른 대기가 예약으로 승격되고 남은 대기 순번이 재계산된다")
    void cancelReservationPromotesFirstWaitingAndRecalculatesRank() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        long reservationId = ((Number) createReservation(brownToken, date, timeId, themeId)
                .getBody()
                .get("id")).longValue();
        createWaiting(conyToken, date, timeId, themeId);
        createWaiting(sallyToken, date, timeId, themeId);

        ResponseEntity<Void> response = cancelReservation(brownToken, reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        List<Map<String, Object>> brownMine = findMine(brownToken);
        assertThat(brownMine).isEmpty();

        List<Map<String, Object>> conyMine = findMine(conyToken);
        assertThat(conyMine).hasSize(1);
        assertThat(conyMine.getFirst().get("status")).isEqualTo("RESERVED");
        assertThat(conyMine.getFirst().get("rank")).isNull();

        List<Map<String, Object>> sallyMine = findMine(sallyToken);
        assertThat(sallyMine).hasSize(1);
        assertThat(sallyMine.getFirst().get("status")).isEqualTo("WAITING");
        assertThat(sallyMine.getFirst().get("rank")).isEqualTo(1);
    }
}

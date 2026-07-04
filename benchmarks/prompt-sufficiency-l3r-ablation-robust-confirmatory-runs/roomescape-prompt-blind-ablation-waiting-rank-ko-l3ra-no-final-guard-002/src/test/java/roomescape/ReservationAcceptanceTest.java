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
    @DisplayName("이미 예약된 슬롯에 대기를 신청하면 내 대기 순번을 반환한다")
    void occupiedSlotWaitingReturnsRank() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> response = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("WAITING");
        assertThat(response.getBody().get("rank")).isEqualTo(1);
    }

    @Test
    @DisplayName("예약 가능한 빈 슬롯에는 대기를 신청할 수 없다")
    void availableSlotWaitingIsRejected() {
        createMember("코니", "cony@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> response = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("WAITING_NOT_ALLOWED");
    }

    @Test
    @DisplayName("본인의 예약에는 대기를 신청할 수 없다")
    void ownReservationWaitingIsRejected() {
        createMember("브라운", "brown@example.com");
        String brownToken = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> response = createWaiting(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("OWN_RESERVATION_WAITING");
    }

    @Test
    @DisplayName("같은 회원은 같은 슬롯에 중복 대기를 신청할 수 없다")
    void duplicateWaitingIsRejected() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);
        createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> response = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("DUPLICATE_WAITING");
    }

    @Test
    @DisplayName("대기 순번은 신청 순서와 결정적인 tie-breaker로 계산한다")
    void waitingRankOrderingFollowsCreateOrder() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> first = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);
        ResponseEntity<Map> second = createWaiting(sallyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(first.getBody().get("rank")).isEqualTo(1);
        assertThat(second.getBody().get("rank")).isEqualTo(2);
    }

    @Test
    @DisplayName("내 예약 목록에서는 예약과 대기를 함께 보여 주고 대기 항목에는 현재 순번을 표시한다")
    void myListContainsReservationsAndWaitingsWithWaitingRank() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long tenId = createTime(LocalTime.of(10, 0));
        long elevenId = createTime(LocalTime.of(11, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), tenId, themeId);
        createReservation(conyToken, LocalDate.of(2030, 5, 2), elevenId, themeId);
        createWaiting(conyToken, LocalDate.of(2030, 5, 1), tenId, themeId);
        createWaiting(sallyToken, LocalDate.of(2030, 5, 1), tenId, themeId);

        List<Map<String, Object>> mine = findMine(conyToken);

        assertThat(mine).hasSize(2);
        assertThat(mine).extracting(item -> item.get("status"))
                .containsExactly("WAITING", "RESERVED");
        assertThat(mine.getFirst().get("rank")).isEqualTo(1);
        assertThat(mine.get(1).get("rank")).isNull();
    }
}

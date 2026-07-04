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
    @DisplayName("이미 예약된 슬롯에는 대기를 신청하고 내 대기 순번을 확인할 수 있다")
    void occupiedSlotWaiting() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);

        ResponseEntity<Map> waiting = createWaiting(conyToken, date, timeId, themeId);

        assertThat(waiting.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(waiting.getBody().get("status")).isEqualTo("WAITING");
        assertThat(waiting.getBody().get("rank")).isEqualTo(1);
    }

    @Test
    @DisplayName("예약 가능한 빈 슬롯에는 대기를 신청할 수 없다")
    void availableSlotWaitingIsRejected() {
        createMember("코니", "cony@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> waiting = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(waiting.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(waiting.getBody().get("code")).isEqualTo("AVAILABLE_SLOT_WAITING");
    }

    @Test
    @DisplayName("본인의 예약에는 대기를 신청할 수 없다")
    void ownReservationWaitingIsRejected() {
        createMember("브라운", "brown@example.com");
        String brownToken = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);

        ResponseEntity<Map> waiting = createWaiting(brownToken, date, timeId, themeId);

        assertThat(waiting.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(waiting.getBody().get("code")).isEqualTo("OWN_RESERVATION_WAITING");
    }

    @Test
    @DisplayName("같은 회원은 같은 슬롯에 중복으로 대기할 수 없다")
    void duplicateWaitingIsRejected() {
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
    @DisplayName("대기 순번은 신청 순서와 식별자 tie-breaker로 계산된다")
    void rankOrdering() {
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

        ResponseEntity<Map> first = createWaiting(conyToken, date, timeId, themeId);
        ResponseEntity<Map> second = createWaiting(sallyToken, date, timeId, themeId);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().get("rank")).isEqualTo(1);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getBody().get("rank")).isEqualTo(2);
    }

    @Test
    @DisplayName("내 예약 목록은 예약과 대기를 함께 보여 주고 대기 항목의 현재 순번을 표시한다")
    void myListResponseIncludesReservationsAndWaitings() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long waitingTimeId = createTime(LocalTime.of(10, 0));
        long reservationTimeId = createTime(LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, waitingTimeId, themeId);
        createWaiting(sallyToken, date, waitingTimeId, themeId);
        createWaiting(conyToken, date, waitingTimeId, themeId);
        createReservation(conyToken, date, reservationTimeId, themeId);

        List<Map<String, Object>> mine = findMine(conyToken);

        assertThat(mine).hasSize(2);
        assertThat(mine)
                .extracting(item -> item.get("status"))
                .containsExactly("WAITING", "RESERVATION");
        assertThat(mine.getFirst().get("rank")).isEqualTo(2);
        assertThat(mine.get(1).get("rank")).isNull();
    }
}

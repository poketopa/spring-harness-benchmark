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
    @DisplayName("이미 예약된 슬롯에 대기를 신청하면 신청 순서대로 순번을 반환한다")
    void createWaitingForOccupiedSlotAndRankByOrder() {
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

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().get("rank")).isEqualTo(1);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getBody().get("rank")).isEqualTo(2);
    }

    @Test
    @DisplayName("예약되지 않은 슬롯에는 대기할 수 없다")
    void availableSlotWaitingReturnsBadRequest() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> response = createWaiting(token, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("AVAILABLE_SLOT_WAITING");
    }

    @Test
    @DisplayName("본인의 예약에는 대기할 수 없다")
    void ownReservationWaitingReturnsBadRequest() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        createReservation(token, LocalDate.of(2030, 5, 1), timeId, themeId);

        ResponseEntity<Map> response = createWaiting(token, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("OWN_RESERVATION_WAITING");
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

        ResponseEntity<Map> response = createWaiting(conyToken, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("DUPLICATE_WAITING");
    }

    @Test
    @DisplayName("내 예약 목록에서 예약과 대기를 함께 조회하고 대기 순번을 표시한다")
    void myListContainsReservationsAndWaitingsWithRank() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long firstTimeId = createTime(LocalTime.of(10, 0));
        long secondTimeId = createTime(LocalTime.of(12, 0));
        createReservation(brownToken, LocalDate.of(2030, 5, 1), firstTimeId, themeId);
        createReservation(conyToken, LocalDate.of(2030, 5, 2), secondTimeId, themeId);
        createWaiting(sallyToken, LocalDate.of(2030, 5, 1), firstTimeId, themeId);
        createWaiting(conyToken, LocalDate.of(2030, 5, 1), firstTimeId, themeId);

        List<Map<String, Object>> mine = findMine(conyToken);

        assertThat(mine).hasSize(2);
        assertThat(mine)
                .extracting(item -> item.get("status"))
                .containsExactly("WAITING", "RESERVED");
        assertThat(mine.getFirst().get("rank")).isEqualTo(2);
        assertThat(mine.get(1).get("rank")).isNull();
    }
}

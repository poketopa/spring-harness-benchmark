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
    @DisplayName("이미 예약된 슬롯에는 대기를 신청할 수 있고 순번을 반환한다")
    void occupiedSlotWaiting() {
        String brownToken = createMemberAndLogin("브라운", "brown@example.com");
        String conyToken = createMemberAndLogin("코니", "cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);

        ResponseEntity<Map> response = createWaiting(conyToken, date, timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("WAITING");
        assertThat(response.getBody().get("waitingRank")).isEqualTo(1);
    }

    @Test
    @DisplayName("예약 가능한 빈 슬롯에는 대기를 신청할 수 없다")
    void availableSlotRejection() {
        String token = createMemberAndLogin("브라운", "brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> response = createWaiting(token, LocalDate.of(2030, 5, 1), timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("WAITING_NOT_ALLOWED");
    }

    @Test
    @DisplayName("본인의 예약에는 대기를 신청할 수 없다")
    void ownReservationRejection() {
        String token = createMemberAndLogin("브라운", "brown@example.com");
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
    void duplicateWaitingRejection() {
        String brownToken = createMemberAndLogin("브라운", "brown@example.com");
        String conyToken = createMemberAndLogin("코니", "cony@example.com");
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
    @DisplayName("대기 순번은 신청 순서와 결정적인 tie-breaker로 계산한다")
    void rankOrdering() {
        String brownToken = createMemberAndLogin("브라운", "brown@example.com");
        String conyToken = createMemberAndLogin("코니", "cony@example.com");
        String sallyToken = createMemberAndLogin("샐리", "sally@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);

        ResponseEntity<Map> first = createWaiting(conyToken, date, timeId, themeId);
        ResponseEntity<Map> second = createWaiting(sallyToken, date, timeId, themeId);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody().get("waitingRank")).isEqualTo(1);
        assertThat(second.getBody().get("waitingRank")).isEqualTo(2);
    }

    @Test
    @DisplayName("내 예약 목록은 예약과 대기를 함께 보여 주고 대기 항목에는 현재 순번을 표시한다")
    void myListResponse() {
        String brownToken = createMemberAndLogin("브라운", "brown@example.com");
        String conyToken = createMemberAndLogin("코니", "cony@example.com");
        long themeId = createTheme("어둠의 방");
        long reservedTimeId = createTime(LocalTime.of(10, 0));
        long myTimeId = createTime(LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, reservedTimeId, themeId);
        createWaiting(conyToken, date, reservedTimeId, themeId);
        createReservation(conyToken, date, myTimeId, themeId);

        List<Map<String, Object>> mine = findMine(conyToken);

        assertThat(mine).hasSize(2);
        assertThat(mine)
                .extracting(item -> item.get("status"))
                .containsExactly("WAITING", "RESERVED");
        assertThat(mine.getFirst().get("waitingRank")).isEqualTo(1);
    }

    private String createMemberAndLogin(String name, String email) {
        createMember(name, email);
        return login(email);
    }
}

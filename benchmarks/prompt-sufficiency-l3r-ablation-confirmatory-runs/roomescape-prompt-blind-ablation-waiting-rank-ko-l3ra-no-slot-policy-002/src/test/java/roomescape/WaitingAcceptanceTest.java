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
    @DisplayName("예약이 차 있는 슬롯에 대기를 신청하면 내 대기 순번을 보여준다")
    void occupiedSlotWaiting() {
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
        assertThat(response.getBody().get("status")).isEqualTo("WAITING");
        assertThat(response.getBody().get("rank")).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 회원은 같은 슬롯에 중복 대기할 수 없다")
    void duplicateWaitingRejection() {
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
    @DisplayName("대기 순번은 신청 순서와 식별자 tie-breaker로 결정된다")
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

        assertThat(first.getBody().get("rank")).isEqualTo(1);
        assertThat(second.getBody().get("rank")).isEqualTo(2);
    }

    @Test
    @DisplayName("내 예약 목록에는 예약과 대기가 함께 보이고 대기 항목은 현재 순번을 표시한다")
    void myListResponse() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long reservedTimeId = createTime(LocalTime.of(10, 0));
        long myTimeId = createTime(LocalTime.of(12, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, reservedTimeId, themeId);
        createWaiting(conyToken, date, reservedTimeId, themeId);
        createWaiting(sallyToken, date, reservedTimeId, themeId);
        createReservation(conyToken, date, myTimeId, themeId);

        List<Map<String, Object>> mine = findMine(conyToken);

        assertThat(mine).hasSize(2);
        assertThat(mine)
                .extracting(item -> item.get("status"))
                .containsExactly("WAITING", "RESERVED");
        assertThat(mine.getFirst().get("rank")).isEqualTo(1);
        assertThat(mine.get(1).get("rank")).isNull();
    }
}

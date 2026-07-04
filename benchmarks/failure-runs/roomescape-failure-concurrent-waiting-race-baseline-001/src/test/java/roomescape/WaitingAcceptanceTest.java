package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
    @DisplayName("예약된 슬롯에 대기를 신청하면 현재 대기 순번을 반환한다")
    void createWaitingReturnsCurrentRank() {
        String reservedToken = createMemberAndLogin("브라운", "brown@example.com");
        String waitingToken = createMemberAndLogin("코니", "cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(reservedToken, date, timeId, themeId);

        ResponseEntity<Map> response = createWaiting(waitingToken, date, timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("status")).isEqualTo("WAITING");
        assertThat(response.getBody().get("waitingRank")).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 회원은 같은 슬롯에 중복 대기할 수 없다")
    void duplicateWaitingReturnsConflict() {
        String reservedToken = createMemberAndLogin("브라운", "brown@example.com");
        String waitingToken = createMemberAndLogin("코니", "cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(reservedToken, date, timeId, themeId);
        createWaiting(waitingToken, date, timeId, themeId);

        ResponseEntity<Map> response = createWaiting(waitingToken, date, timeId, themeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("code")).isEqualTo("DUPLICATE_WAITING");
    }

    @Test
    @DisplayName("회원은 자신의 대기를 취소할 수 있다")
    void cancelOwnWaiting() {
        String reservedToken = createMemberAndLogin("브라운", "brown@example.com");
        String waitingToken = createMemberAndLogin("코니", "cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(reservedToken, date, timeId, themeId);
        ResponseEntity<Map> waiting = createWaiting(waitingToken, date, timeId, themeId);
        long waitingId = ((Number) waiting.getBody().get("id")).longValue();

        ResponseEntity<Void> response = cancelWaiting(waitingToken, waitingId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(waitingToken)).isEmpty();
    }

    @Test
    @DisplayName("내 예약 목록은 확정 예약과 대기 예약을 상태와 대기 순번으로 함께 보여준다")
    void findMineIncludesConfirmedReservationsAndWaitings() {
        String ownerToken = createMemberAndLogin("브라운", "brown@example.com");
        String otherToken = createMemberAndLogin("코니", "cony@example.com");
        long themeId = createTheme("어둠의 방");
        long reservedTimeId = createTime(LocalTime.of(10, 0));
        long waitingTimeId = createTime(LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(ownerToken, date, reservedTimeId, themeId);
        createReservation(otherToken, date, waitingTimeId, themeId);
        createWaiting(ownerToken, date, waitingTimeId, themeId);

        List<Map<String, Object>> mine = findMine(ownerToken);

        assertThat(mine).hasSize(2);
        assertThat(mine)
                .extracting(item -> item.get("status"))
                .containsExactly("CONFIRMED", "WAITING");
        assertThat(mine.get(0).get("waitingRank")).isNull();
        assertThat(mine.get(1).get("waitingRank")).isEqualTo(1);
    }

    @Test
    @DisplayName("앞 순번 대기가 취소되면 뒤 순번 대기의 순번이 다시 계산된다")
    void waitingRankIsRecalculatedAfterCancellation() {
        String reservedToken = createMemberAndLogin("브라운", "brown@example.com");
        String firstToken = createMemberAndLogin("코니", "cony@example.com");
        String secondToken = createMemberAndLogin("샐리", "sally@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(reservedToken, date, timeId, themeId);
        ResponseEntity<Map> firstWaiting = createWaiting(firstToken, date, timeId, themeId);
        ResponseEntity<Map> secondWaiting = createWaiting(secondToken, date, timeId, themeId);
        long firstWaitingId = ((Number) firstWaiting.getBody().get("id")).longValue();
        assertThat(secondWaiting.getBody().get("waitingRank")).isEqualTo(2);

        cancelWaiting(firstToken, firstWaitingId);

        List<Map<String, Object>> mine = findMine(secondToken);
        assertThat(mine).hasSize(1);
        assertThat(mine.getFirst().get("waitingRank")).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 회원의 같은 슬롯 대기 신청이 동시에 들어와도 하나만 생성된다")
    void concurrentDuplicateWaitingCreatesOnlyOne() throws Exception {
        String reservedToken = createMemberAndLogin("브라운", "brown@example.com");
        String waitingToken = createMemberAndLogin("코니", "cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(reservedToken, date, timeId, themeId);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        List<ResponseEntity<Map>> responses = java.util.Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < 2; i++) {
            executorService.submit(() -> {
                ready.countDown();
                start.await();
                responses.add(createWaiting(waitingToken, date, timeId, themeId));
                return null;
            });
        }
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        executorService.shutdown();
        assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(responses).hasSize(2);
        assertThat(responses)
                .extracting(ResponseEntity::getStatusCode)
                .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
        assertThat(findMine(waitingToken)).hasSize(1);
    }

    private String createMemberAndLogin(String name, String email) {
        createMember(name, email);
        return login(email);
    }
}

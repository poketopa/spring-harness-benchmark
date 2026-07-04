package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ConcurrentLoginAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("새 로그인이 성공하면 이전 토큰은 사용할 수 없고 새 토큰만 사용할 수 있다")
    void newestLoginWins() {
        createMember("브라운", "brown@example.com");
        String firstToken = login("brown@example.com");

        String secondToken = login("brown@example.com");

        assertThat(firstToken).isNotEqualTo(secondToken);
        assertTokenStatus(firstToken, HttpStatus.UNAUTHORIZED);
        assertTokenStatus(secondToken, HttpStatus.OK);
    }

    @Test
    @DisplayName("동시 로그인 후 하나의 토큰만 활성 상태로 남는다")
    void concurrentLoginLeavesOnlyOneActiveToken() throws Exception {
        createMember("브라운", "brown@example.com");
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<ResponseEntity<Map>> first = executorService.submit(() -> loginAfterStart(ready, start));
        Future<ResponseEntity<Map>> second = executorService.submit(() -> loginAfterStart(ready, start));
        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        ResponseEntity<Map> firstResponse = first.get(3, TimeUnit.SECONDS);
        ResponseEntity<Map> secondResponse = second.get(3, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> tokens = List.of(
                (String) firstResponse.getBody().get("accessToken"),
                (String) secondResponse.getBody().get("accessToken")
        );
        assertThat(tokens.get(0)).isNotEqualTo(tokens.get(1));
        long activeCount = tokens.stream()
                .map(this::tokenStatus)
                .filter(HttpStatus.OK::equals)
                .count();
        assertThat(activeCount).isEqualTo(1);
    }

    private ResponseEntity<Map> loginAfterStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return loginResponse("brown@example.com");
    }

    private ResponseEntity<Map> loginResponse(String email) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", request, Map.class);
    }

    private void assertTokenStatus(String token, HttpStatus expected) {
        assertThat(tokenStatus(token)).isEqualTo(expected);
    }

    private HttpStatus tokenStatus(String token) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
        return HttpStatus.valueOf(response.getStatusCode().value());
    }
}

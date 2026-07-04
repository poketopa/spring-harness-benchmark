package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
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
    @DisplayName("같은 계정으로 다시 로그인하면 이전 토큰은 무효화되고 새 토큰만 사용할 수 있다")
    void newLoginInvalidatesPreviousToken() {
        // given
        createMember("브라운", "brown@example.com");
        String firstToken = login("brown@example.com");

        // when
        String secondToken = login("brown@example.com");
        ResponseEntity<Map> oldTokenResponse = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(firstToken)),
                Map.class
        );
        ResponseEntity<List> newTokenResponse = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(secondToken)),
                List.class
        );

        // then
        assertThat(secondToken).isNotEqualTo(firstToken);
        assertThat(oldTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(oldTokenResponse.getBody().get("code")).isEqualTo("UNAUTHORIZED");
        assertThat(newTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("세션이 없는 같은 계정의 동시 로그인 요청은 서버 오류 없이 하나의 활성 토큰만 남긴다")
    void simultaneousFirstLoginLeavesOneActiveToken() throws Exception {
        // given
        createMember("브라운", "brown@example.com");
        int loginCount = 2;
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(loginCount);

        try {
            List<Future<ResponseEntity<Map>>> futures = IntStream.range(0, loginCount)
                    .mapToObj(index -> executor.submit(() -> loginAfterStartSignal(ready, start)))
                    .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            // when
            start.countDown();
            List<ResponseEntity<Map>> responses = collectResponses(futures);

            // then
            assertThat(responses).allSatisfy(response ->
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK));
            List<String> tokens = responses.stream()
                    .map(response -> (String) response.getBody().get("accessToken"))
                    .toList();
            assertThat(tokens).doesNotHaveDuplicates();

            List<Integer> statusCodes = tokens.stream()
                    .map(this::findMineStatusCode)
                    .toList();
            assertThat(statusCodes).containsExactlyInAnyOrder(
                    HttpStatus.OK.value(),
                    HttpStatus.UNAUTHORIZED.value()
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private ResponseEntity<Map> loginAfterStartSignal(CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        Map<String, Object> request = Map.of(
                "email", "brown@example.com",
                "password", "password"
        );
        return restTemplate.postForEntity("/login", request, Map.class);
    }

    private List<ResponseEntity<Map>> collectResponses(List<Future<ResponseEntity<Map>>> futures) throws Exception {
        List<ResponseEntity<Map>> responses = new ArrayList<>();
        for (Future<ResponseEntity<Map>> future : futures) {
            responses.add(future.get(5, TimeUnit.SECONDS));
        }
        return responses;
    }

    private int findMineStatusCode(String token) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
        return response.getStatusCode().value();
    }
}

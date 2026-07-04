package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하다")
    void onlyNewestLoginTokenWorksForSameMember() {
        createMember("브라운", "brown@example.com");
        String oldToken = login("brown@example.com");
        String newToken = login("brown@example.com");

        ResponseEntity<Map> oldTokenResponse = findMineWithBody(oldToken);
        ResponseEntity<List> newTokenResponse = findMineResponse(newToken);

        assertThat(oldToken).isNotEqualTo(newToken);
        assertThat(oldTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(oldTokenResponse.getBody().get("code")).isEqualTo("UNAUTHORIZED");
        assertThat(newTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("다른 회원의 토큰은 새 로그인에 영향을 받지 않는다")
    void otherMemberTokenRemainsValid() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");

        String newBrownToken = login("brown@example.com");

        assertThat(findMineWithBody(brownToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineResponse(newBrownToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(conyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인은 서버 오류 없이 하나의 최신 세션만 남긴다")
    void concurrentSameMemberLoginDoesNotExposeServerError() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(loginCount);
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();

        for (int i = 0; i < loginCount; i++) {
            futures.add(executor.submit(concurrentLoginTask(ready, start)));
        }
        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<String> tokens = new ArrayList<>();
        for (Future<ResponseEntity<Map>> future : futures) {
            ResponseEntity<Map> response = future.get(5, TimeUnit.SECONDS);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            tokens.add((String) response.getBody().get("accessToken"));
        }
        executor.shutdownNow();

        long activeTokenCount = tokens.stream()
                .map(this::findMineStatus)
                .filter(response -> response.getStatusCode() == HttpStatus.OK)
                .count();

        assertThat(tokens).doesNotHaveDuplicates();
        assertThat(activeTokenCount).isEqualTo(1);
    }

    private Callable<ResponseEntity<Map>> concurrentLoginTask(CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertThat(start.await(3, TimeUnit.SECONDS)).isTrue();
            return loginResponse("brown@example.com");
        };
    }

    private ResponseEntity<Map> loginResponse(String email) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", request, Map.class);
    }

    private ResponseEntity<List> findMineResponse(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                List.class
        );
    }

    private ResponseEntity<String> findMineStatus(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
    }

    private ResponseEntity<Map> findMineWithBody(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }
}

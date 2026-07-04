package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class MemberAuthenticationAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("회원가입한 회원은 로그인할 수 있다")
    void memberSignsUpAndLogsIn() {
        Map<String, Object> signupRequest = Map.of(
                "name", "브라운",
                "email", "brown@example.com",
                "password", "password"
        );
        ResponseEntity<Map> signup = restTemplate.postForEntity("/members", signupRequest, Map.class);
        Map<String, Object> loginRequest = Map.of(
                "email", "brown@example.com",
                "password", "password"
        );

        ResponseEntity<Map> login = restTemplate.postForEntity("/login", loginRequest, Map.class);

        assertThat(signup.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody().get("accessToken")).isInstanceOf(String.class);
    }

    @Test
    @DisplayName("같은 회원이 다시 로그인하면 이전 토큰은 사용할 수 없다")
    void previousTokenIsInvalidatedAfterLoginAgain() {
        createMember("브라운", "brown@example.com");
        String firstToken = login("brown@example.com");

        String secondToken = login("brown@example.com");

        assertThat(firstToken).isNotEqualTo(secondToken);
        assertThat(findMineStatus(firstToken)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineStatus(secondToken)).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 회원이 동시에 로그인해도 하나의 최신 토큰만 사용할 수 있다")
    void onlyOneLatestTokenIsValidAfterConcurrentLogin() throws Exception {
        createMember("브라운", "brown@example.com");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        try {
            Future<String> firstLogin = executorService.submit(() -> loginAfterStart(ready, start));
            Future<String> secondLogin = executorService.submit(() -> loginAfterStart(ready, start));
            ready.await();

            start.countDown();
            String firstToken = firstLogin.get();
            String secondToken = secondLogin.get();

            assertThat(firstToken).isNotEqualTo(secondToken);
            assertThat(List.of(findMineStatus(firstToken), findMineStatus(secondToken)))
                    .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.UNAUTHORIZED);
        } finally {
            executorService.shutdownNow();
        }
    }

    private String loginAfterStart(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        start.await();
        return login("brown@example.com");
    }

    private HttpStatus findMineStatus(String token) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
        return HttpStatus.valueOf(response.getStatusCode().value());
    }
}

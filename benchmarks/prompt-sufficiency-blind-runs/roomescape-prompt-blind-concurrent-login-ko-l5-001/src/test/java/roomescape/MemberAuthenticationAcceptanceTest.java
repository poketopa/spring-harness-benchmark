package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하고 이전 토큰은 인증 실패한다")
    void newLoginInvalidatesPreviousToken() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String previousBrownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");

        String currentBrownToken = login("brown@example.com");

        assertThat(currentBrownToken).isNotEqualTo(previousBrownToken);
        assertThat(findMineResponse(currentBrownToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(previousBrownToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineResponse(conyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 회원의 실제 동시 로그인에서도 한 토큰만 유효하고 500 응답이 발생하지 않는다")
    void concurrentLoginsKeepOnlyOneActiveToken() throws Exception {
        createMember("브라운", "brown@example.com");
        Map<String, Object> loginRequest = Map.of(
                "email", "brown@example.com",
                "password", "password"
        );
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<ResponseEntity<Map>> loginTask = () -> {
            ready.countDown();
            start.await(3, TimeUnit.SECONDS);
            return restTemplate.postForEntity("/login", loginRequest, Map.class);
        };

        try {
            Future<ResponseEntity<Map>> firstLogin = executorService.submit(loginTask);
            Future<ResponseEntity<Map>> secondLogin = executorService.submit(loginTask);
            assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            ResponseEntity<Map> firstResponse = firstLogin.get(5, TimeUnit.SECONDS);
            ResponseEntity<Map> secondResponse = secondLogin.get(5, TimeUnit.SECONDS);
            String firstToken = (String) firstResponse.getBody().get("accessToken");
            String secondToken = (String) secondResponse.getBody().get("accessToken");

            assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(firstToken).isNotEqualTo(secondToken);
            HttpStatus firstTokenStatus = HttpStatus.valueOf(findMineResponse(firstToken).getStatusCode().value());
            HttpStatus secondTokenStatus = HttpStatus.valueOf(findMineResponse(secondToken).getStatusCode().value());
            assertThat(firstTokenStatus)
                    .isIn(HttpStatus.OK, HttpStatus.UNAUTHORIZED);
            assertThat(secondTokenStatus)
                    .isIn(HttpStatus.OK, HttpStatus.UNAUTHORIZED);
            assertThat(List.of(firstTokenStatus, secondTokenStatus))
                    .containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.UNAUTHORIZED);
        } finally {
            executorService.shutdownNow();
        }
    }

    private ResponseEntity<String> findMineResponse(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
    }
}

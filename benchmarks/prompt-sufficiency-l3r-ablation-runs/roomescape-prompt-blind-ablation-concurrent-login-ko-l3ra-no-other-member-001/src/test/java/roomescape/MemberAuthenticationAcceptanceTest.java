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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하고 이전 토큰은 인증에 실패한다")
    void onlyLatestLoginTokenIsValid() {
        createMember("브라운", "brown@example.com");
        String oldToken = login("brown@example.com");

        String newToken = login("brown@example.com");

        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(findMineResponse(newToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(oldToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인은 서버 오류 없이 하나의 토큰만 활성화한다")
    void concurrentLoginKeepsOnlyOneActiveToken() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<ResponseEntity<Map>>> futures = IntStream.range(0, loginCount)
                    .mapToObj(index -> executorService.submit(() -> {
                        ready.countDown();
                        assertThat(start.await(3, TimeUnit.SECONDS)).isTrue();
                        return loginResponse("brown@example.com");
                    }))
                    .toList();

            assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<ResponseEntity<Map>> loginResponses = new ArrayList<>();
            for (Future<ResponseEntity<Map>> future : futures) {
                loginResponses.add(future.get(3, TimeUnit.SECONDS));
            }

            assertThat(loginResponses)
                    .allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK));

            List<String> tokens = loginResponses.stream()
                    .map(response -> (String) response.getBody().get("accessToken"))
                    .toList();
            List<ResponseEntity<String>> authenticationResponses = tokens.stream()
                    .map(this::findMineResponse)
                    .toList();

            assertThat(authenticationResponses)
                    .allSatisfy(response -> assertThat(response.getStatusCode().is5xxServerError()).isFalse());
            assertThat(authenticationResponses.stream()
                    .filter(response -> response.getStatusCode().equals(HttpStatus.OK))
                    .count()).isEqualTo(1);
            assertThat(authenticationResponses.stream()
                    .filter(response -> response.getStatusCode().equals(HttpStatus.UNAUTHORIZED))
                    .count()).isEqualTo(loginCount - 1);
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰은 인증에 실패한다")
    void invalidTokenFailsAuthentication() {
        createMember("브라운", "brown@example.com");

        ResponseEntity<String> response = findMineResponse("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Map> loginResponse(String email) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", request, Map.class);
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

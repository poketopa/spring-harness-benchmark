package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하고 이전 토큰은 인증에 실패한다")
    void onlyLatestLoginTokenIsValid() {
        createMember("브라운", "brown@example.com");
        String oldToken = login("brown@example.com");
        String newToken = login("brown@example.com");

        ResponseEntity<String> oldTokenResponse = findMineResponse(oldToken);
        ResponseEntity<String> newTokenResponse = findMineResponse(newToken);

        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(oldTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(newTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("잘못된 토큰은 인증에 실패한다")
    void invalidTokenFailsAuthentication() {
        ResponseEntity<String> response = findMineResponse("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인은 500 없이 처리되고 하나의 토큰만 유효하다")
    void concurrentLoginsKeepOnlyOneTokenActive() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);
        CyclicBarrier barrier = new CyclicBarrier(loginCount);
        List<Callable<ResponseEntity<Map>>> tasks = new ArrayList<>();
        for (int i = 0; i < loginCount; i++) {
            tasks.add(() -> {
                barrier.await();
                return requestLogin("brown@example.com");
            });
        }

        List<Future<ResponseEntity<Map>>> futures = executorService.invokeAll(tasks);
        executorService.shutdown();

        List<String> tokens = new ArrayList<>();
        for (Future<ResponseEntity<Map>> future : futures) {
            ResponseEntity<Map> response = future.get();
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            tokens.add((String) response.getBody().get("accessToken"));
        }
        long validTokenCount = tokens.stream()
                .map(this::findMineResponse)
                .filter(response -> response.getStatusCode().is2xxSuccessful())
                .count();

        assertThat(tokens).doesNotHaveDuplicates();
        assertThat(validTokenCount).isEqualTo(1);
    }

    private ResponseEntity<Map> requestLogin(String email) {
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

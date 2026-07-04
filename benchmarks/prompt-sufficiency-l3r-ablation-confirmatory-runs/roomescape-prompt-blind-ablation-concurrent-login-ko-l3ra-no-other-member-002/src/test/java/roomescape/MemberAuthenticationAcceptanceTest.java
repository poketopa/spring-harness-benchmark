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
    void onlyLatestLoginTokenIsValid() {
        createMember("브라운", "brown@example.com");
        String oldToken = login("brown@example.com");

        String newToken = login("brown@example.com");

        ResponseEntity<Map> oldTokenResponse = findMineErrorResponse(oldToken);
        ResponseEntity<Void> newTokenResponse = findMineStatus(newToken);
        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(newTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(oldTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(oldTokenResponse.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("유효하지 않은 토큰은 인증 실패한다")
    void invalidTokenFailsAuthentication() {
        createMember("브라운", "brown@example.com");

        ResponseEntity<Map> response = findMineErrorResponse("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인에서도 500 없이 하나의 최신 토큰만 유효하다")
    void concurrentLoginsKeepOnlyOneLatestToken() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < loginCount; i++) {
                futures.add(executorService.submit(() -> {
                    ready.countDown();
                    start.await();
                    return loginResponse("brown@example.com");
                }));
            }

            assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<ResponseEntity<Map>> responses = new ArrayList<>();
            for (Future<ResponseEntity<Map>> future : futures) {
                responses.add(future.get(5, TimeUnit.SECONDS));
            }

            List<String> tokens = responses.stream()
                    .peek(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK))
                    .map(response -> (String) response.getBody().get("accessToken"))
                    .toList();
            List<Integer> authenticationStatuses = tokens.stream()
                    .map(token -> findMineStatus(token).getStatusCode().value())
                    .toList();

            assertThat(tokens).doesNotHaveDuplicates();
            assertThat(authenticationStatuses).containsOnly(HttpStatus.OK.value(), HttpStatus.UNAUTHORIZED.value());
            assertThat(authenticationStatuses).filteredOn(status -> status == HttpStatus.OK.value()).hasSize(1);
            assertThat(authenticationStatuses).filteredOn(status -> status == HttpStatus.UNAUTHORIZED.value())
                    .hasSize(loginCount - 1);
        } finally {
            executorService.shutdownNow();
        }
    }

    private ResponseEntity<Map> loginResponse(String email) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", request, Map.class);
    }

    private ResponseEntity<Void> findMineStatus(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }

    private ResponseEntity<Map> findMineErrorResponse(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );
    }
}

package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하고 이전 토큰은 인증 실패한다")
    void onlyLatestLoginTokenIsValid() {
        createMember("브라운", "brown@example.com");

        String oldToken = login("brown@example.com");
        String latestToken = login("brown@example.com");

        assertThat(findMineResponse(latestToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(oldToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원의 재로그인은 다른 회원의 토큰에 영향을 주지 않는다")
    void otherMemberTokenRemainsValid() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");

        login("cony@example.com");
        login("cony@example.com");

        assertThat(findMineResponse(brownToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("잘못된 토큰은 인증 실패로 응답한다")
    void invalidTokenFailsAuthentication() {
        createMember("브라운", "brown@example.com");

        ResponseEntity<Object> response = findMineResponse("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인에서도 서버 오류 없이 하나의 토큰만 유효하다")
    void concurrentLoginKeepsOnlyOneTokenValid() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 2;
        CyclicBarrier barrier = new CyclicBarrier(loginCount);
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);

        try {
            List<Future<ResponseEntity<Map>>> futures = List.of(
                    executorService.submit(concurrentLoginTask(barrier, "brown@example.com")),
                    executorService.submit(concurrentLoginTask(barrier, "brown@example.com"))
            );

            List<ResponseEntity<Map>> responses = futures.stream()
                    .map(this::get)
                    .toList();

            assertThat(responses)
                    .extracting(ResponseEntity::getStatusCode)
                    .allSatisfy(status -> assertThat(status.is5xxServerError()).isFalse())
                    .containsOnly(HttpStatus.OK);

            List<String> tokens = responses.stream()
                    .map(response -> (String) response.getBody().get("accessToken"))
                    .toList();

            long validTokenCount = tokens.stream()
                    .map(this::findMineResponse)
                    .filter(response -> response.getStatusCode() == HttpStatus.OK)
                    .count();

            assertThat(validTokenCount).isEqualTo(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    private Callable<ResponseEntity<Map>> concurrentLoginTask(CyclicBarrier barrier, String email) {
        return () -> {
            barrier.await();
            return loginResponse(email);
        };
    }

    private ResponseEntity<Map> loginResponse(String email) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", request, Map.class);
    }

    private ResponseEntity<Object> findMineResponse(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Object.class
        );
    }

    private ResponseEntity<Map> get(Future<ResponseEntity<Map>> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}

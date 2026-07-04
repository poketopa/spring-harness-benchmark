package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

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
        String previousToken = login("brown@example.com");

        String latestToken = login("brown@example.com");

        assertThat(latestToken).isNotEqualTo(previousToken);
        assertThat(findMineWithToken(previousToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineWithToken(latestToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("한 회원의 재로그인은 다른 회원의 기존 토큰에 영향을 주지 않는다")
    void otherMembersTokenRemainsValid() {
        createMember("브라운", "brown@example.com");
        createMember("샐리", "sally@example.com");
        String brownPreviousToken = login("brown@example.com");
        String sallyToken = login("sally@example.com");

        String brownLatestToken = login("brown@example.com");

        assertThat(findMineWithToken(brownPreviousToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineWithToken(brownLatestToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineWithToken(sallyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인은 서버 오류 없이 처리되고 토큰 하나만 유효하다")
    void concurrentLoginKeepsOnlyOneTokenValid() throws Exception {
        createMember("브라운", "brown@example.com");
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<ResponseEntity<Map>> first = executorService.submit(() -> loginAtSameTime(ready, start));
            Future<ResponseEntity<Map>> second = executorService.submit(() -> loginAtSameTime(ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            start.countDown();
            ResponseEntity<Map> firstLogin = first.get(5, TimeUnit.SECONDS);
            ResponseEntity<Map> secondLogin = second.get(5, TimeUnit.SECONDS);

            assertThat(firstLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(secondLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
            String firstToken = accessTokenOf(firstLogin);
            String secondToken = accessTokenOf(secondLogin);
            assertThat(firstToken).isNotEqualTo(secondToken);
            assertThat(isAuthenticated(firstToken) ^ isAuthenticated(secondToken)).isTrue();
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰은 인증 실패한다")
    void invalidTokenFailsAuthentication() {
        createMember("브라운", "brown@example.com");

        ResponseEntity<String> response = findMineWithToken("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("\"code\":\"UNAUTHORIZED\"");
    }

    private ResponseEntity<Map> loginAtSameTime(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
        return loginResponse("brown@example.com");
    }

    private ResponseEntity<Map> loginResponse(String email) {
        Map<String, Object> loginRequest = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", loginRequest, Map.class);
    }

    private String accessTokenOf(ResponseEntity<Map> response) {
        return (String) response.getBody().get("accessToken");
    }

    private boolean isAuthenticated(String token) {
        return findMineWithToken(token).getStatusCode().is2xxSuccessful();
    }

    private ResponseEntity<String> findMineWithToken(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
    }
}

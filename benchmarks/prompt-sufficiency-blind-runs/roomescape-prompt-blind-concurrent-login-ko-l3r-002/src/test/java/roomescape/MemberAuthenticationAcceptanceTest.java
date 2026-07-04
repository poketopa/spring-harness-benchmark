package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
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

    private ExecutorService executorService;

    @AfterEach
    void tearDown() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

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
    void onlyLatestLoginTokenIsValidForSameMember() {
        createMember("브라운", "brown@example.com");
        String firstToken = login("brown@example.com");
        String latestToken = login("brown@example.com");

        ResponseEntity<String> latestTokenResponse = findMineWithoutAssertion(latestToken);
        ResponseEntity<String> firstTokenResponse = findMineWithoutAssertion(firstToken);

        assertThat(latestTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(firstTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("한 회원의 재로그인은 다른 회원의 토큰에 영향을 주지 않는다")
    void anotherMembersTokenRemainsValidAfterLogin() {
        createMember("브라운", "brown@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String sallyToken = login("sally@example.com");

        String latestBrownToken = login("brown@example.com");

        assertThat(findMineWithoutAssertion(latestBrownToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineWithoutAssertion(brownToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineWithoutAssertion(sallyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("잘못된 토큰은 인증 실패로 응답한다")
    void invalidTokenFailsAuthentication() {
        ResponseEntity<String> response = findMineWithoutAssertion("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원의 실제 동시 로그인은 500 없이 처리되고 하나의 토큰만 최종 유효하다")
    void concurrentLoginKeepsOnlyOneActiveSession() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 4;
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        executorService = Executors.newFixedThreadPool(loginCount);

        List<Future<ResponseEntity<Map>>> futures = IntStream.range(0, loginCount)
                .mapToObj(index -> executorService.submit(() -> {
                    ready.countDown();
                    start.await();
                    return loginWithoutAssertion("brown@example.com");
                }))
                .toList();

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        List<ResponseEntity<Map>> responses = futures.stream()
                .map(future -> get(future, 5, TimeUnit.SECONDS))
                .toList();

        assertThat(responses)
                .extracting(ResponseEntity::getStatusCode)
                .containsOnly(HttpStatus.OK);
        List<String> tokens = responses.stream()
                .map(response -> (String) response.getBody().get("accessToken"))
                .toList();
        long validTokenCount = tokens.stream()
                .map(this::findMineWithoutAssertion)
                .filter(response -> response.getStatusCode().isSameCodeAs(HttpStatus.OK))
                .count();

        assertThat(validTokenCount).isEqualTo(1);
    }

    private ResponseEntity<Map> loginWithoutAssertion(String email) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", request, Map.class);
    }

    private ResponseEntity<String> findMineWithoutAssertion(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
    }

    private ResponseEntity<Map> get(Future<ResponseEntity<Map>> future, long timeout, TimeUnit unit) {
        try {
            return future.get(timeout, unit);
        } catch (Exception exception) {
            throw new AssertionError("로그인 요청이 제한 시간 안에 완료되지 않았습니다.", exception);
        }
    }
}

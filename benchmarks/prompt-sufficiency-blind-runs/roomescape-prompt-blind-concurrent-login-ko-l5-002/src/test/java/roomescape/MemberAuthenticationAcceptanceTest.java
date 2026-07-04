package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 사용할 수 있다")
    void onlyLatestTokenIsValidForSameMember() {
        createMember("브라운", "brown@example.com");
        String oldToken = login("brown@example.com");

        String newToken = login("brown@example.com");

        assertThat(newToken).isNotEqualTo(oldToken);
        assertFindMineStatus(oldToken, HttpStatus.UNAUTHORIZED);
        assertFindMineStatus(newToken, HttpStatus.OK);
    }

    @Test
    @DisplayName("한 회원의 재로그인은 다른 회원의 토큰에 영향을 주지 않는다")
    void otherMemberTokenStaysValidAfterRelogin() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");

        String renewedBrownToken = login("brown@example.com");

        assertThat(renewedBrownToken).isNotEqualTo(brownToken);
        assertFindMineStatus(brownToken, HttpStatus.UNAUTHORIZED);
        assertFindMineStatus(renewedBrownToken, HttpStatus.OK);
        assertFindMineStatus(conyToken, HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 회원의 실제 동시 로그인은 500 없이 하나의 최신 토큰만 남긴다")
    void concurrentLoginKeepsSingleActiveToken() throws InterruptedException {
        createMember("브라운", "brown@example.com");
        int loginCount = 8;
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);
        List<ResponseEntity<Map>> responses = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < loginCount; i++) {
            executorService.submit(() -> {
                ready.countDown();
                await(start);
                responses.add(requestLogin("brown@example.com"));
            });
        }

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        executorService.shutdown();
        assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(responses).hasSize(loginCount);
        assertThat(responses)
                .extracting(ResponseEntity::getStatusCode)
                .containsOnly(HttpStatus.OK);

        List<String> tokens = responses.stream()
                .map(response -> (String) response.getBody().get("accessToken"))
                .toList();
        long validTokenCount = tokens.stream()
                .filter(token -> findMineStatus(token) == HttpStatus.OK)
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

    private void assertFindMineStatus(String token, HttpStatus status) {
        assertThat(findMineStatus(token)).isEqualTo(status);
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

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}

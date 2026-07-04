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
    @DisplayName("같은 계정으로 다시 로그인하면 이전 토큰은 사용할 수 없다")
    void previousTokenIsRejectedAfterSameMemberLogsInAgain() {
        createMember("브라운", "brown@example.com");
        String previousToken = login("brown@example.com");
        String latestToken = login("brown@example.com");

        ResponseEntity<Map> previousTokenResponse = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(previousToken)),
                Map.class
        );
        ResponseEntity<Object[]> latestTokenResponse = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(latestToken)),
                Object[].class
        );

        assertThat(latestToken).isNotEqualTo(previousToken);
        assertThat(previousTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(previousTokenResponse.getBody().get("code")).isEqualTo("UNAUTHORIZED");
        assertThat(latestTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 계정으로 동시에 여러 번 로그인해도 하나의 토큰만 유효하다")
    void onlyOneTokenIsValidAfterConcurrentLogins() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < loginCount; i++) {
            futures.add(executorService.submit(() -> {
                startLatch.await();
                return login("brown@example.com");
            }));
        }

        startLatch.countDown();
        List<String> tokens = new ArrayList<>();
        for (Future<String> future : futures) {
            tokens.add(future.get(5, TimeUnit.SECONDS));
        }
        executorService.shutdown();

        long validTokenCount = tokens.stream()
                .map(this::findMineStatus)
                .filter(HttpStatus.OK::equals)
                .count();
        long rejectedTokenCount = tokens.stream()
                .map(this::findMineStatus)
                .filter(HttpStatus.UNAUTHORIZED::equals)
                .count();

        assertThat(validTokenCount).isEqualTo(1);
        assertThat(rejectedTokenCount).isEqualTo(loginCount - 1);
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

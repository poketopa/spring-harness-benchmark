package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하고 이전 토큰은 인증 실패한다")
    void onlyLatestLoginTokenIsValidForSameMember() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String oldToken = login("brown@example.com");
        String otherMemberToken = login("cony@example.com");

        String newToken = login("brown@example.com");

        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(findMineResponse(newToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(oldToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineResponse(otherMemberToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰은 인증 실패한다")
    void invalidTokenFailsAuthentication() {
        ResponseEntity<String> response = findMineResponse("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원이 동시에 여러 번 로그인해도 활성 토큰은 하나만 남는다")
    void concurrentLoginsLeaveOnlyOneActiveTokenForSameMember() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 6;
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);

        try {
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < loginCount; i++) {
                futures.add(executorService.submit(loginAfterStart(ready, start)));
            }
            ready.await();
            start.countDown();

            List<String> tokens = new ArrayList<>();
            for (Future<String> future : futures) {
                tokens.add(future.get());
            }

            long validTokenCount = tokens.stream()
                    .map(this::findMineResponse)
                    .filter(response -> response.getStatusCode().is2xxSuccessful())
                    .count();

            assertThat(tokens).doesNotHaveDuplicates();
            assertThat(validTokenCount).isEqualTo(1);
        } finally {
            executorService.shutdownNow();
        }
    }

    private Callable<String> loginAfterStart(CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            start.await();
            return login("brown@example.com");
        };
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

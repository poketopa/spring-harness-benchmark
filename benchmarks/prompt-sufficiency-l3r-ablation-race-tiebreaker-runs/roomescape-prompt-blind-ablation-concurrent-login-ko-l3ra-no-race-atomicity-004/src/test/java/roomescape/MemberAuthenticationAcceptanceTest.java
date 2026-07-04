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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하고 이전 토큰은 인증에 실패한다")
    void repeatedLoginInvalidatesPreviousTokenOnlyForSameMember() {
        createMember("브라운", "brown@example.com");
        createMember("샐리", "sally@example.com");
        String previousToken = login("brown@example.com");
        String otherMemberToken = login("sally@example.com");

        String newToken = login("brown@example.com");

        assertThat(newToken).isNotEqualTo(previousToken);
        assertThat(findMineStatus(newToken)).isEqualTo(HttpStatus.OK);
        assertThat(findMineStatus(previousToken)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineStatus(otherMemberToken)).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("유효하지 않은 토큰은 인증에 실패한다")
    void invalidTokenFailsAuthentication() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders("invalid-token")),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인 후에도 하나의 토큰만 유효하다")
    void concurrentLoginKeepsOnlyOneActiveTokenForSameMember() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < loginCount; i++) {
                futures.add(executorService.submit(() -> {
                    ready.countDown();
                    start.await();
                    return login("brown@example.com");
                }));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<String> tokens = new ArrayList<>();
            for (Future<String> future : futures) {
                tokens.add(future.get(5, TimeUnit.SECONDS));
            }

            long activeTokenCount = tokens.stream()
                    .map(this::findMineStatus)
                    .filter(HttpStatus.OK::equals)
                    .count();
            assertThat(tokens).doesNotHaveDuplicates();
            assertThat(activeTokenCount).isEqualTo(1);
        } finally {
            executorService.shutdownNow();
        }
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

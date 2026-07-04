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
    void repeatedLoginInvalidatesPreviousToken() {
        createMember("브라운", "brown@example.com");
        String previousToken = login("brown@example.com");

        String newToken = login("brown@example.com");

        assertThat(newToken).isNotEqualTo(previousToken);
        assertThat(findMineResponse(newToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<String> previousTokenResponse = findMineResponse(previousToken);
        assertThat(previousTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(previousTokenResponse.getBody()).contains("UNAUTHORIZED");
    }

    @Test
    @DisplayName("다른 회원이 로그인해도 기존 회원 토큰은 유지된다")
    void otherMemberLoginDoesNotInvalidateToken() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");

        String conyToken = login("cony@example.com");

        assertThat(findMineResponse(brownToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(conyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 회원이 동시에 로그인해도 서버 오류 없이 하나의 최종 토큰만 유지된다")
    void concurrentLoginKeepsOneActiveTokenWithoutServerError() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 8;
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(loginCount);

        List<Callable<ResponseEntity<Map>>> tasks = new ArrayList<>();
        for (int i = 0; i < loginCount; i++) {
            tasks.add(() -> {
                ready.countDown();
                start.await(3, TimeUnit.SECONDS);
                return loginResponse("brown@example.com");
            });
        }

        List<Future<ResponseEntity<Map>>> futures = tasks.stream()
                .map(executor::submit)
                .toList();
        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<String> tokens = new ArrayList<>();
        for (Future<ResponseEntity<Map>> future : futures) {
            ResponseEntity<Map> response = future.get(5, TimeUnit.SECONDS);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            tokens.add((String) response.getBody().get("accessToken"));
        }
        executor.shutdown();
        assertThat(executor.awaitTermination(3, TimeUnit.SECONDS)).isTrue();

        long activeTokenCount = tokens.stream()
                .map(this::findMineResponse)
                .filter(response -> response.getStatusCode() == HttpStatus.OK)
                .count();
        long unauthorizedTokenCount = tokens.stream()
                .map(this::findMineResponse)
                .filter(response -> response.getStatusCode() == HttpStatus.UNAUTHORIZED)
                .count();

        assertThat(tokens).doesNotHaveDuplicates();
        assertThat(activeTokenCount).isEqualTo(1);
        assertThat(unauthorizedTokenCount).isEqualTo(loginCount - 1);
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

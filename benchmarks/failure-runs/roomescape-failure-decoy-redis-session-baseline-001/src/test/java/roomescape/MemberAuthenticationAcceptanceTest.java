package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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
    @DisplayName("같은 회원의 로그인 토큰은 로그인마다 새로 발급된다")
    void loginIssuesUniqueTokens() {
        createMember("브라운", "brown@example.com");

        String firstToken = login("brown@example.com");
        String secondToken = login("brown@example.com");

        assertThat(secondToken).isNotEqualTo(firstToken);
    }

    @Test
    @DisplayName("같은 회원이 다시 로그인하면 이전 토큰은 인증에 실패한다")
    void staleTokenIsRejectedAfterNewLogin() {
        createMember("브라운", "brown@example.com");
        String firstToken = login("brown@example.com");
        String secondToken = login("brown@example.com");

        ResponseEntity<String> staleResponse = findMineResponse(firstToken);
        ResponseEntity<String> currentResponse = findMineResponse(secondToken);

        assertThat(staleResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(currentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("현재 로그인 토큰은 인증이 필요한 요청에 사용할 수 있다")
    void currentTokenAuthorizesRequests() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");

        ResponseEntity<String> response = findMineResponse(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 회원의 반복 로그인은 서버 오류 없이 마지막 토큰만 유효하게 유지한다")
    void repeatedLoginKeepsNewestTokenCurrentWithoutServerError() {
        createMember("브라운", "brown@example.com");
        List<String> tokens = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            tokens.add(login("brown@example.com"));
        }

        for (int i = 0; i < tokens.size() - 1; i++) {
            ResponseEntity<String> staleResponse = findMineResponse(tokens.get(i));
            assertThat(staleResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        ResponseEntity<String> currentResponse = findMineResponse(tokens.getLast());
        assertThat(currentResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인 요청은 서버 오류를 반환하지 않는다")
    void concurrentLoginDoesNotReturnServerError() throws Exception {
        createMember("브라운", "brown@example.com");
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        try {
            List<Callable<ResponseEntity<Map>>> tasks = List.of(
                    () -> requestLogin("brown@example.com"),
                    () -> requestLogin("brown@example.com"),
                    () -> requestLogin("brown@example.com"),
                    () -> requestLogin("brown@example.com")
            );

            List<Future<ResponseEntity<Map>>> futures = executorService.invokeAll(tasks);

            for (Future<ResponseEntity<Map>> future : futures) {
                ResponseEntity<Map> response = future.get();
                assertThat(response.getStatusCode().is5xxServerError()).isFalse();
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody().get("accessToken")).isInstanceOf(String.class);
            }
        } finally {
            executorService.shutdownNow();
        }
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

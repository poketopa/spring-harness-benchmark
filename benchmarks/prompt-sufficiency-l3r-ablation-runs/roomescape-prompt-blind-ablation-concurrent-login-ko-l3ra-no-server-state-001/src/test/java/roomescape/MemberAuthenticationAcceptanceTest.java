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
    void onlyLatestLoginTokenIsValidForSameMember() {
        createMember("브라운", "brown@example.com");

        String oldToken = login("brown@example.com");
        String newToken = login("brown@example.com");

        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(findMineResponse(newToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(oldToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("한 회원의 재로그인은 다른 회원의 토큰에 영향을 주지 않는다")
    void otherMemberTokenRemainsValidAfterMemberLogsInAgain() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownOldToken = login("brown@example.com");
        String conyToken = login("cony@example.com");

        String brownNewToken = login("brown@example.com");

        assertThat(findMineResponse(brownOldToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineResponse(brownNewToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(conyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("유효하지 않은 토큰은 인증 실패한다")
    void invalidTokenFailsAuthentication() {
        ResponseEntity<String> response = findMineResponse("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원이 동시에 로그인해도 서버 오류 없이 가장 최근 세션 하나만 유효하다")
    void concurrentSameMemberLoginDoesNotExposeServerError() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 6;
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();
        for (int i = 0; i < loginCount; i++) {
            futures.add(executorService.submit(() -> {
                ready.countDown();
                start.await();
                return requestLogin("brown@example.com");
            }));
        }

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<ResponseEntity<Map>> loginResponses = new ArrayList<>();
        for (Future<ResponseEntity<Map>> future : futures) {
            loginResponses.add(future.get(5, TimeUnit.SECONDS));
        }
        executorService.shutdown();
        assertThat(executorService.awaitTermination(3, TimeUnit.SECONDS)).isTrue();

        assertThat(loginResponses)
                .allSatisfy(response -> assertThat(response.getStatusCode().is5xxServerError()).isFalse())
                .allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK));

        List<String> tokens = loginResponses.stream()
                .map(response -> (String) response.getBody().get("accessToken"))
                .toList();
        long validTokenCount = tokens.stream()
                .map(this::findMineResponse)
                .filter(response -> response.getStatusCode().is2xxSuccessful())
                .count();

        assertThat(tokens).doesNotHaveDuplicates();
        assertThat(validTokenCount).isEqualTo(1);
    }

    private ResponseEntity<Map> requestLogin(String email) {
        Map<String, Object> loginRequest = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", loginRequest, Map.class);
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

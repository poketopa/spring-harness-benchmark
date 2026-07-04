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
    void tearDownExecutor() {
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
    @DisplayName("같은 회원이 다시 로그인하면 최신 토큰만 인증된다")
    void onlyLatestTokenAuthenticatesForSameMember() {
        createMember("브라운", "brown@example.com");
        String previousToken = login("brown@example.com");
        String latestToken = login("brown@example.com");

        assertThat(latestToken).isNotEqualTo(previousToken);
        assertThat(findMineResponse(latestToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(previousToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("한 회원의 재로그인은 다른 회원의 토큰에 영향을 주지 않는다")
    void otherMemberTokenRemainsValidAfterMemberRelogin() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String previousBrownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String latestBrownToken = login("brown@example.com");

        assertThat(findMineResponse(latestBrownToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(previousBrownToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineResponse(conyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 인증 실패 응답을 반환한다")
    void invalidTokenReturnsUnauthorized() {
        ResponseEntity<String> response = findMineResponse("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인은 500 없이 처리되고 하나의 토큰만 활성 상태가 된다")
    void concurrentLoginsLeaveOnlyOneActiveToken() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 8;
        executorService = Executors.newFixedThreadPool(loginCount);
        CountDownLatch ready = new CountDownLatch(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();

        for (int i = 0; i < loginCount; i++) {
            futures.add(executorService.submit(() -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("동시 로그인 시작 신호를 받지 못했습니다.");
                }
                return loginResponse("brown@example.com");
            }));
        }

        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        List<ResponseEntity<Map>> responses = new ArrayList<>();
        for (Future<ResponseEntity<Map>> future : futures) {
            responses.add(future.get(10, TimeUnit.SECONDS));
        }

        assertThat(responses).allSatisfy(response -> {
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().get("accessToken")).isInstanceOf(String.class);
        });

        List<String> tokens = responses.stream()
                .map(response -> (String) response.getBody().get("accessToken"))
                .toList();
        List<Integer> authenticationStatuses = tokens.stream()
                .map(token -> findMineResponse(token).getStatusCode().value())
                .toList();

        assertThat(authenticationStatuses.stream()
                .filter(status -> status == HttpStatus.OK.value())
                .count()).isEqualTo(1);
        assertThat(authenticationStatuses.stream()
                .filter(status -> status == HttpStatus.UNAUTHORIZED.value())
                .count()).isEqualTo(loginCount - 1);
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

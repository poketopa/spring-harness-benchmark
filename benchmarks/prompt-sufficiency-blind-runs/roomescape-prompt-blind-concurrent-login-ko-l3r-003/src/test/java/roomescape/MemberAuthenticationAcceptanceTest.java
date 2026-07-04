package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하고 이전 토큰은 실패한다")
    void onlyLatestTokenIsValidForSameMember() {
        createMember("브라운", "brown@example.com");
        String previousToken = login("brown@example.com");

        String latestToken = login("brown@example.com");

        assertThat(latestToken).isNotEqualTo(previousToken);
        assertThat(requestMine(latestToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(requestMine(previousToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("한 회원의 재로그인은 다른 회원 토큰에 영향을 주지 않는다")
    void otherMemberTokenStaysValidWhenMemberLogsInAgain() {
        createMember("브라운", "brown@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String sallyToken = login("sally@example.com");

        String latestBrownToken = login("brown@example.com");

        assertThat(latestBrownToken).isNotEqualTo(brownToken);
        assertThat(requestMine(latestBrownToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(requestMine(brownToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(requestMine(sallyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인은 서버 오류 없이 하나의 토큰만 활성화한다")
    void concurrentLoginsOnlyKeepOneActiveToken() throws Exception {
        createMember("브라운", "brown@example.com");
        int attemptCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(attemptCount);

        try {
            CyclicBarrier barrier = new CyclicBarrier(attemptCount);
            Callable<ResponseEntity<Map>> task = () -> {
                barrier.await(5, TimeUnit.SECONDS);
                return loginResponse("brown@example.com");
            };

            List<Future<ResponseEntity<Map>>> futures = new ArrayList<>();
            for (int i = 0; i < attemptCount; i++) {
                futures.add(executor.submit(task));
            }

            List<ResponseEntity<Map>> responses = new ArrayList<>();
            for (Future<ResponseEntity<Map>> future : futures) {
                responses.add(future.get(5, TimeUnit.SECONDS));
            }

            assertThat(responses).allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK));
            List<String> tokens = responses.stream()
                    .map(response -> (String) response.getBody().get("accessToken"))
                    .toList();
            assertThat(tokens).doesNotHaveDuplicates();

            List<HttpStatus> authenticationResults = tokens.stream()
                    .map(token -> (HttpStatus) requestMine(token).getStatusCode())
                    .toList();
            assertThat(authenticationResults).containsOnly(HttpStatus.OK, HttpStatus.UNAUTHORIZED);
            assertThat(authenticationResults).filteredOn(HttpStatus.OK::equals).hasSize(1);
            assertThat(authenticationResults).filteredOn(HttpStatus.UNAUTHORIZED::equals).hasSize(attemptCount - 1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰은 인증 실패로 처리한다")
    void invalidTokenFailsAuthentication() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders("invalid-token")),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    private ResponseEntity<Map> loginResponse(String email) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", request, Map.class);
    }

    private ResponseEntity<String> requestMine(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
    }
}

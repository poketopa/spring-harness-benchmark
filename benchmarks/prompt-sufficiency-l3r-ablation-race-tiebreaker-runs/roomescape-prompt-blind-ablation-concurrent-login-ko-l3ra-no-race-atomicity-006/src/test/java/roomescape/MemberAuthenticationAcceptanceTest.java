package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하고 이전 토큰은 거부된다")
    void onlyLatestTokenIsValidForSameMember() {
        createMember("브라운", "brown@example.com");
        String oldToken = login("brown@example.com");
        String newToken = login("brown@example.com");

        ResponseEntity<String> newTokenResponse = findMineResponse(newToken);
        ResponseEntity<String> oldTokenResponse = findMineResponse(oldToken);

        assertThat(newToken).isNotEqualTo(oldToken);
        assertThat(newTokenResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(oldTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("한 회원의 재로그인은 다른 회원의 토큰에 영향을 주지 않는다")
    void renewingOneMemberTokenDoesNotInvalidateAnotherMemberToken() {
        createMember("브라운", "brown@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String sallyToken = login("sally@example.com");

        String renewedBrownToken = login("brown@example.com");

        assertThat(findMineResponse(renewedBrownToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(brownToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineResponse(sallyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("잘못된 토큰은 인증 실패로 거부된다")
    void invalidTokenIsRejected() {
        ResponseEntity<String> response = findMineResponse("invalid-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인 후에는 하나의 최신 세션만 남는다")
    void concurrentLoginLeavesOnlyOneActiveSession() throws Exception {
        createMember("브라운", "brown@example.com");
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<String> loginTask = () -> {
            start.await(3, TimeUnit.SECONDS);
            return login("brown@example.com");
        };

        Future<String> firstLogin = executorService.submit(loginTask);
        Future<String> secondLogin = executorService.submit(loginTask);
        start.countDown();
        String firstToken = firstLogin.get(5, TimeUnit.SECONDS);
        String secondToken = secondLogin.get(5, TimeUnit.SECONDS);
        executorService.shutdown();

        long activeTokenCount = java.util.stream.Stream.of(firstToken, secondToken)
                .map(this::findMineResponse)
                .filter(response -> response.getStatusCode().is2xxSuccessful())
                .count();

        assertThat(firstToken).isNotEqualTo(secondToken);
        assertThat(activeTokenCount).isEqualTo(1);
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

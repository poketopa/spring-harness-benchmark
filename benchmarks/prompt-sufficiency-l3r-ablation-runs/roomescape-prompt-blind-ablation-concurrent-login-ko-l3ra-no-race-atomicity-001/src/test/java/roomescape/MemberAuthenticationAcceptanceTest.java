package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    @DisplayName("같은 회원이 다시 로그인하면 새 토큰만 유효하고 기존 토큰은 인증 실패한다")
    void onlyLatestTokenIsValidForSameMember() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String oldBrownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");

        String newBrownToken = login("brown@example.com");

        assertThat(newBrownToken).isNotEqualTo(oldBrownToken);
        assertThat(findMineResponse(newBrownToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse(oldBrownToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineResponse(conyToken).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findMineResponse("invalid-token").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("같은 회원의 동시 로그인 후에도 서버에 저장된 활성 세션 토큰 하나만 인증된다")
    void concurrentLoginLeavesOnlyOneActiveSession() throws Exception {
        createMember("브라운", "brown@example.com");
        int loginCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(loginCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < loginCount; i++) {
            futures.add(executorService.submit(() -> {
                start.await();
                return login("brown@example.com");
            }));
        }

        start.countDown();
        List<String> tokens = new ArrayList<>();
        for (Future<String> future : futures) {
            tokens.add(future.get());
        }
        executorService.shutdown();

        long activeTokenCount = tokens.stream()
                .map(this::findMineResponse)
                .filter(response -> response.getStatusCode().is2xxSuccessful())
                .count();

        assertThat(tokens).doesNotHaveDuplicates();
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

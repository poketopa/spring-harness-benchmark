package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    @DisplayName("새 로그인이 성공하면 같은 회원의 이전 토큰은 사용할 수 없다")
    void newLoginInvalidatesPreviousToken() {
        createMember("브라운", "brown@example.com");
        String firstToken = login("brown@example.com");

        String secondToken = login("brown@example.com");

        assertThat(secondToken).isNotEqualTo(firstToken);
        assertUnauthorized(firstToken);
        assertThat(findMine(secondToken)).isEmpty();
    }

    @Test
    @DisplayName("로그인 토큰은 로그인한 회원의 식별 정보로 인증된다")
    void loginTokenAuthenticatesMemberIdentity() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(java.time.LocalTime.of(10, 0));
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");

        ResponseEntity<Map> response = createReservation(
                brownToken,
                java.time.LocalDate.now().plusDays(1),
                timeId,
                themeId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(findMine(brownToken)).hasSize(1);
        assertThat(findMine(conyToken)).isEmpty();
    }

    @Test
    @DisplayName("동시에 같은 회원으로 로그인해도 하나의 최신 토큰만 활성 상태로 남는다")
    void concurrentLoginKeepsOnlyOneActiveToken() throws Exception {
        createMember("브라운", "brown@example.com");
        int threadCount = 2;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<String> tokens = java.util.Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                ready.countDown();
                start.await();
                tokens.add(login("brown@example.com"));
                return null;
            });
        }

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        executorService.shutdown();
        assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        assertThat(tokens).hasSize(threadCount);
        assertThat(new HashSet<>(tokens)).hasSize(threadCount);
        assertThat(tokens.stream().filter(this::isActiveToken)).hasSize(1);
    }

    private boolean isActiveToken(String token) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
        return response.getStatusCode() == HttpStatus.OK;
    }

    private void assertUnauthorized(String token) {
        ResponseEntity<String> response = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

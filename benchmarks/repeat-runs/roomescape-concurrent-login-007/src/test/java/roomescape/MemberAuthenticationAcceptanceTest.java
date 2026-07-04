package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

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
    void newLoginInvalidatesPreviousToken() {
        createMember("브라운", "brown@example.com");
        String firstToken = login("brown@example.com");

        String secondToken = login("brown@example.com");

        assertThat(firstToken).isNotEqualTo(secondToken);
        assertThat(findMineStatus(firstToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(findMineStatus(secondToken).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("동시에 같은 회원으로 로그인해도 하나의 토큰만 활성 상태로 남는다")
    void concurrentLoginLeavesOnlyOneActiveToken() throws Exception {
        createMember("브라운", "brown@example.com");
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<ResponseEntity<Map>> loginTask = () -> {
            ready.countDown();
            start.await(1, TimeUnit.SECONDS);
            return loginWithResponse("brown@example.com");
        };

        Future<ResponseEntity<Map>> first = executorService.submit(loginTask);
        Future<ResponseEntity<Map>> second = executorService.submit(loginTask);
        assertThat(ready.await(1, TimeUnit.SECONDS)).isTrue();

        start.countDown();

        ResponseEntity<Map> firstLogin = first.get(5, TimeUnit.SECONDS);
        ResponseEntity<Map> secondLogin = second.get(5, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(firstLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondLogin.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> tokens = List.of(
                (String) firstLogin.getBody().get("accessToken"),
                (String) secondLogin.getBody().get("accessToken")
        );
        assertThat(tokens.get(0)).isNotEqualTo(tokens.get(1));
        long activeCount = tokens.stream()
                .map(this::findMineStatus)
                .filter(response -> response.getStatusCode().is2xxSuccessful())
                .count();
        long inactiveCount = tokens.stream()
                .map(this::findMineStatus)
                .filter(response -> response.getStatusCode() == HttpStatus.UNAUTHORIZED)
                .count();
        assertThat(activeCount).isEqualTo(1);
        assertThat(inactiveCount).isEqualTo(1);
    }

    private ResponseEntity<Map> loginWithResponse(String email) {
        Map<String, Object> loginRequest = Map.of(
                "email", email,
                "password", "password"
        );
        return restTemplate.postForEntity("/login", loginRequest, Map.class);
    }

    private ResponseEntity<String> findMineStatus(String token) {
        return restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                String.class
        );
    }
}

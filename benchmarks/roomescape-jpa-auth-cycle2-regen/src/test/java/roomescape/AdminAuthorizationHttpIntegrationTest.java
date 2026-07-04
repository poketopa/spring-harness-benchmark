package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.Map;
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
class AdminAuthorizationHttpIntegrationTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("어드민 API는 인증이 없으면 인증 실패 응답을 반환한다")
    void adminApiWithoutTokenReturnsUnauthorized() {
        Map<String, Object> request = Map.of("startAt", LocalTime.of(10, 0).toString());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/times",
                HttpMethod.POST,
                new HttpEntity<>(request),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("어드민 API는 일반 회원이면 권한 실패 응답을 반환한다")
    void adminApiWithMemberTokenReturnsForbidden() {
        createMember("코니", "cony@example.com");
        String token = login("cony@example.com");
        Map<String, Object> request = Map.of("startAt", LocalTime.of(10, 0).toString());

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/times",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }
}

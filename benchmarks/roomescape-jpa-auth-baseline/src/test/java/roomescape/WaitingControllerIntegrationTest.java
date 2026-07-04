package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

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
class WaitingControllerIntegrationTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("대기 신청 요청에 필수값이 없으면 입력 오류 응답을 반환한다")
    void createWaitingWithInvalidRequestReturnsBadRequest() {
        createMember("코니", "cony@example.com");
        String token = login("cony@example.com");
        Map<String, Object> request = Map.of(
                "timeId", 1L,
                "themeId", 1L
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/reservations/waitings",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_INPUT");
    }
}

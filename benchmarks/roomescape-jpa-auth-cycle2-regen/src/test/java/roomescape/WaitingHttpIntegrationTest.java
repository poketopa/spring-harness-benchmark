package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
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
class WaitingHttpIntegrationTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("대기 생성 요청 값이 누락되면 검증 실패 응답을 반환한다")
    void invalidWaitingRequestReturnsBadRequest() {
        createMember("코니", "cony@example.com");
        String token = login("cony@example.com");
        Map<String, Object> request = Map.of(
                "timeId", 1L,
                "themeId", 1L
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/waitings",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("code")).isEqualTo("INVALID_INPUT");
    }

    @Test
    @DisplayName("다른 회원의 대기를 취소하면 찾을 수 없음 응답을 반환한다")
    void cancelOtherMemberWaitingReturnsNotFound() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);
        long waitingId = ((Number) createWaiting(conyToken, date, timeId, themeId).getBody().get("id")).longValue();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/waitings/" + waitingId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(sallyToken)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("WAITING_NOT_FOUND");
    }
}

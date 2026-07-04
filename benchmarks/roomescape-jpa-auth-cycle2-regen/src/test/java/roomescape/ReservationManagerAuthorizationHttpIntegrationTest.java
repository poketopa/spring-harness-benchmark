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
class ReservationManagerAuthorizationHttpIntegrationTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("인증 없이 매니저 예약 목록을 조회하면 인증 실패 응답을 반환한다")
    void unauthenticatedManagedReservationRequestReturnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                null,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("일반 회원이 매니저 예약 목록을 조회하면 인가 실패 응답을 반환한다")
    void memberManagedReservationRequestReturnsForbidden() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }
}

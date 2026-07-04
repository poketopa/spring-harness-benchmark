package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
class JpaAddOnAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("표준 경로로 내 예약과 대기를 조회하고 대기를 취소할 수 있다")
    void memberUsesStandardWaitingAndMinePaths() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);
        long waitingId = ((Number) createWaitingByStandardPath(conyToken, date, timeId, themeId)
                .getBody()
                .get("id")).longValue();

        ResponseEntity<List> mineResponse = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(conyToken)),
                List.class
        );
        ResponseEntity<Void> cancelResponse = restTemplate.exchange(
                "/waitings/" + waitingId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(conyToken)),
                Void.class
        );

        assertThat(mineResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(mineResponse.getBody()).hasSize(1);
        Map<String, Object> waiting = (Map<String, Object>) mineResponse.getBody().getFirst();
        assertThat(waiting.get("status")).isEqualTo("WAITING");
        assertThat(waiting.get("rank")).isEqualTo(1);
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(conyToken)).isEmpty();
    }

    @Test
    @DisplayName("어드민은 전체 대기 목록을 조회하고 대기를 취소할 수 있다")
    void adminFindsAndCancelsWaiting() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        createReservation(brownToken, date, timeId, themeId);
        long waitingId = ((Number) createWaiting(conyToken, date, timeId, themeId).getBody().get("id")).longValue();

        ResponseEntity<List> waitingsResponse = restTemplate.exchange(
                "/admin/waitings",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken())),
                List.class
        );
        ResponseEntity<Void> cancelResponse = restTemplate.exchange(
                "/admin/waitings/" + waitingId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminToken())),
                Void.class
        );

        assertThat(waitingsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(waitingsResponse.getBody()).hasSize(1);
        Map<String, Object> waiting = (Map<String, Object>) waitingsResponse.getBody().getFirst();
        assertThat(((Number) waiting.get("id")).longValue()).isEqualTo(waitingId);
        assertThat(waiting.get("rank")).isEqualTo(1);
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(conyToken)).isEmpty();
    }

    private ResponseEntity<Map> createWaitingByStandardPath(String token, LocalDate date, long timeId, long themeId) {
        Map<String, Object> request = Map.of(
                "date", date.toString(),
                "timeId", timeId,
                "themeId", themeId
        );

        return restTemplate.exchange(
                "/waitings",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)),
                Map.class
        );
    }
}

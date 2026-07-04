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
class ConcurrentLoginAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("같은 계정으로 다시 로그인하면 새 토큰만 예약 생성에 사용할 수 있다")
    void secondLoginInvalidatesFirstToken() {
        createMember("브라운", "brown@example.com");
        String firstToken = login("brown@example.com");
        String secondToken = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));

        ResponseEntity<Map> firstTokenResponse = createReservation(
                firstToken,
                LocalDate.of(2030, 5, 1),
                timeId,
                themeId
        );
        ResponseEntity<Map> secondTokenResponse = createReservation(
                secondToken,
                LocalDate.of(2030, 5, 1),
                timeId,
                themeId
        );
        ResponseEntity<List> secondTokenMine = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(secondToken)),
                List.class
        );

        assertThat(firstTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(firstTokenResponse.getBody().get("code")).isEqualTo("UNAUTHORIZED");
        assertThat(secondTokenResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondTokenMine.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

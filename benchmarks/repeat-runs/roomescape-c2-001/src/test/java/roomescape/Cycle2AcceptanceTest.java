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
class Cycle2AcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("회원은 본인 예약을 변경하고 취소할 수 있다")
    void memberChangesAndCancelsOwnReservation() {
        createMember("브라운", "brown@example.com");
        String token = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long oldTimeId = createTime(LocalTime.of(10, 0));
        long newTimeId = createTime(LocalTime.of(11, 0));
        long reservationId = ((Number) createReservation(token, LocalDate.of(2030, 5, 1), oldTimeId, themeId)
                .getBody()
                .get("id")).longValue();

        ResponseEntity<Map> changed = changeReservation(token, reservationId, LocalDate.of(2030, 5, 2), newTimeId);
        ResponseEntity<Void> cancelled = cancelReservation(token, reservationId);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) newTimeId);
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(token)).isEmpty();
    }

    @Test
    @DisplayName("예약 취소 시 첫 번째 대기가 예약으로 자동 전환되고 다음 대기 순번이 재정렬된다")
    void cancelReservationPromotesFirstWaitingAndReordersRanks() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        createMember("샐리", "sally@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        String sallyToken = login("sally@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        long reservationId = ((Number) createReservation(brownToken, date, timeId, themeId)
                .getBody()
                .get("id")).longValue();
        createWaiting(conyToken, date, timeId, themeId);
        createWaiting(sallyToken, date, timeId, themeId);

        ResponseEntity<Void> cancelled = cancelReservation(brownToken, reservationId);

        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        List<Map<String, Object>> conyMine = findMine(conyToken);
        List<Map<String, Object>> sallyMine = findMine(sallyToken);
        assertThat(conyMine.getFirst().get("status")).isEqualTo("RESERVED");
        assertThat(sallyMine.getFirst().get("status")).isEqualTo("WAITING");
        assertThat(sallyMine.getFirst().get("rank")).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 회원의 예약을 변경하거나 취소하면 찾을 수 없음 응답을 반환한다")
    void nonOwnerReservationMutationReturnsNotFound() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long timeId = createTime(LocalTime.of(10, 0));
        long reservationId = ((Number) createReservation(brownToken, LocalDate.of(2030, 5, 1), timeId, themeId)
                .getBody()
                .get("id")).longValue();

        ResponseEntity<Map> changed = changeReservation(conyToken, reservationId, LocalDate.of(2030, 5, 2), timeId);
        ResponseEntity<Map> cancelled = restTemplate.exchange(
                "/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(conyToken)),
                Map.class
        );

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(changed.getBody().get("code")).isEqualTo("RESERVATION_NOT_FOUND");
        assertThat(cancelled.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(cancelled.getBody().get("code")).isEqualTo("RESERVATION_NOT_FOUND");
    }
}

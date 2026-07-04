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
class ManagerReservationAcceptanceTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약만 조회하고 변경하고 삭제할 수 있다")
    void managerManagesOwnStoreReservations() {
        long managerId = createManager("매니저", "manager@example.com");
        createMember("브라운", "brown@example.com");
        String managerToken = login("manager@example.com");
        String memberToken = login("brown@example.com");
        long storeId = createStore("강남점", managerId);
        long themeId = createTheme("어둠의 방", storeId);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        LocalDate originalDate = LocalDate.of(2030, 5, 1);
        LocalDate changedDate = LocalDate.of(2030, 5, 2);
        long reservationId = ((Number) createReservation(memberToken, originalDate, tenOClockId, themeId)
                .getBody()
                .get("id")).longValue();

        ResponseEntity<List> found = findManagedReservations(managerToken);
        ResponseEntity<Map> changed = changeManagedReservation(
                managerToken,
                reservationId,
                changedDate,
                elevenOClockId
        );
        ResponseEntity<Void> canceled = cancelManagedReservation(managerToken, reservationId);

        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).hasSize(1);
        assertThat(((Map<String, Object>) found.getBody().getFirst()).get("id")).isEqualTo((int) reservationId);
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo(changedDate.toString());
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) elevenOClockId);
        assertThat(canceled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(memberToken)).isEmpty();
    }

    @Test
    @DisplayName("매니저 예약 API는 비로그인, 비매니저, 다른 매장 예약 접근을 거부한다")
    void managerReservationAuthorizationFailures() {
        long managerId = createManager("매니저", "manager@example.com");
        long otherManagerId = createManager("다른매니저", "other-manager@example.com");
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        String otherManagerToken = login("other-manager@example.com");
        long storeId = createStore("강남점", managerId);
        long themeId = createTheme("어둠의 방", storeId);
        long timeId = createTime(LocalTime.of(10, 0));
        long reservationId = ((Number) createReservation(
                memberToken,
                LocalDate.of(2030, 5, 1),
                timeId,
                themeId
        ).getBody().get("id")).longValue();
        createStore("잠실점", otherManagerId);

        ResponseEntity<Map> unauthenticated = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );
        ResponseEntity<Map> userAccess = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(memberToken)),
                Map.class
        );
        ResponseEntity<Map> otherStoreAccess = changeManagedReservation(
                otherManagerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                timeId
        );

        assertThat(unauthenticated.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unauthenticated.getBody().get("code")).isEqualTo("UNAUTHORIZED");
        assertThat(userAccess.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(userAccess.getBody().get("code")).isEqualTo("FORBIDDEN");
        assertThat(otherStoreAccess.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(otherStoreAccess.getBody().get("code")).isEqualTo("FORBIDDEN");
    }
}

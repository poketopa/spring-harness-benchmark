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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationHttpIntegrationTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 조회, 변경, 삭제할 수 있다")
    void managerCanAccessOwnStoreReservations() {
        long managerId = createMember("매니저", "manager@example.com", "MANAGER");
        String managerToken = login("manager@example.com");
        long storeId = createStore("잠실점", managerId);
        long themeId = createTheme("어둠의 방", storeId);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long reservationId = ((Number) createReservation(
                memberToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<List> findResponse = findManagedReservations(managerToken, storeId);
        ResponseEntity<Map> changeResponse = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );
        ResponseEntity<Void> cancelResponse = cancelManagedReservation(managerToken, reservationId);

        assertThat(findResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(findResponse.getBody()).hasSize(1);
        assertThat(changeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changeResponse.getBody().get("startAt")).isEqualTo("11:00:00");
        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    @DisplayName("매니저가 다른 매장의 예약에 접근하면 인가 실패 응답을 반환한다")
    void managerCannotAccessOtherStoreReservation() {
        long managerId = createMember("매니저", "manager@example.com", "MANAGER");
        String managerToken = login("manager@example.com");
        createStore("잠실점", managerId);
        long otherManagerId = createMember("다른매니저", "other-manager@example.com", "MANAGER");
        long otherStoreId = createStore("강남점", otherManagerId);
        long themeId = createTheme("비밀의 방", otherStoreId);
        long timeId = createTime(LocalTime.of(10, 0));
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long reservationId = ((Number) createReservation(
                memberToken,
                LocalDate.of(2030, 5, 1),
                timeId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<Void> response = cancelManagedReservation(managerToken, reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("일반 회원이 매장 예약에 접근하면 인가 실패 응답을 반환한다")
    void nonManagerCannotAccessManagedReservations() {
        long managerId = createMember("매니저", "manager@example.com", "MANAGER");
        long storeId = createStore("잠실점", managerId);
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");

        ResponseEntity<Map> response = findManagedReservationsError(memberToken, storeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 요청이 매장 예약에 접근하면 인증 실패 응답을 반환한다")
    void unauthenticatedCannotAccessManagedReservations() {
        long managerId = createMember("매니저", "manager@example.com", "MANAGER");
        long storeId = createStore("잠실점", managerId);

        ResponseEntity<Map> response = findManagedReservationsWithoutAuth(storeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }
}

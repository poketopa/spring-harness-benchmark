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
    @DisplayName("매니저는 자기 매장 예약만 조회하고 변경할 수 있다")
    void managerFindsAndChangesOwnStoreReservation() {
        // given
        long managerId = createManager("브라운", "brown@example.com");
        long otherManagerId = createManager("코니", "cony@example.com");
        String managerToken = login("brown@example.com");
        String memberToken = createAndLoginMember("샐리", "sally@example.com");
        String otherMemberToken = createAndLoginMember("제임스", "james@example.com");
        long storeId = createStore("강남점", managerId);
        long otherStoreId = createStore("잠실점", otherManagerId);
        long themeId = createTheme("어둠의 방", storeId);
        long otherThemeId = createTheme("빛의 방", otherStoreId);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        LocalDate changedDate = LocalDate.of(2030, 5, 2);
        long reservationId = ((Number) createReservation(memberToken, date, tenOClockId, themeId)
                .getBody()
                .get("id")).longValue();
        createReservation(otherMemberToken, date, tenOClockId, otherThemeId);

        // when
        ResponseEntity<List> managedReservations = findManagedReservations(managerToken);
        ResponseEntity<Map> changed = changeManagedReservation(managerToken, reservationId, changedDate, elevenOClockId);

        // then
        assertThat(managedReservations.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(managedReservations.getBody()).hasSize(1);
        assertThat(((Map<?, ?>) managedReservations.getBody().getFirst()).get("id")).isEqualTo((int) reservationId);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo(changedDate.toString());
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) elevenOClockId);
    }

    private String createAndLoginMember(String name, String email) {
        createMember(name, email);
        return login(email);
    }

    @Test
    @DisplayName("매니저가 다른 매장 예약을 변경하면 인가 실패 응답을 반환한다")
    void managerChangingOtherStoreReservationReturnsForbidden() {
        // given
        long managerId = createManager("브라운", "brown@example.com");
        long otherManagerId = createManager("코니", "cony@example.com");
        String managerToken = login("brown@example.com");
        String memberToken = createAndLoginMember("샐리", "sally@example.com");
        long storeId = createStore("강남점", managerId);
        long otherStoreId = createStore("잠실점", otherManagerId);
        createTheme("어둠의 방", storeId);
        long otherThemeId = createTheme("빛의 방", otherStoreId);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        long reservationId = ((Number) createReservation(memberToken, date, tenOClockId, otherThemeId)
                .getBody()
                .get("id")).longValue();

        // when
        ResponseEntity<Map> response = changeManagedReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("일반 회원이 매니저 예약 API를 호출하면 인가 실패 응답을 반환한다")
    void memberCallingManagerReservationApiReturnsForbidden() {
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

    @Test
    @DisplayName("인증 없이 매니저 예약 API를 호출하면 인증 실패 응답을 반환한다")
    void unauthenticatedManagerReservationApiReturnsUnauthorized() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("매니저는 자기 매장 예약을 취소할 수 있다")
    void managerCancelsOwnStoreReservation() {
        // given
        long managerId = createManager("브라운", "brown@example.com");
        String managerToken = login("brown@example.com");
        String memberToken = createAndLoginMember("샐리", "sally@example.com");
        long storeId = createStore("강남점", managerId);
        long themeId = createTheme("어둠의 방", storeId);
        long timeId = createTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        long reservationId = ((Number) createReservation(memberToken, date, timeId, themeId)
                .getBody()
                .get("id")).longValue();

        // when
        ResponseEntity<Void> response = cancelManagedReservation(managerToken, reservationId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findManagedReservations(managerToken).getBody()).isEmpty();
    }
}

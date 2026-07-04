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
import org.springframework.test.annotation.DirtiesContext;
import roomescape.domain.Role;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationAuthorizationHttpIntegrationTest extends AcceptanceTestSupport {

    @Test
    @DisplayName("매니저는 관리자 예약 조회에서 본인 예약만 볼 수 있다")
    void managerFindsOnlyOwnReservations() {
        // given
        createMember("브라운", "brown@example.com", Role.MANAGER);
        createMember("코니", "cony@example.com", Role.USER);
        String managerToken = login("brown@example.com");
        String userToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        createReservation(managerToken, LocalDate.of(2030, 5, 1), tenOClockId, themeId);
        createReservation(userToken, LocalDate.of(2030, 5, 2), elevenOClockId, themeId);

        // when
        List<Map<String, Object>> reservations = findAdminReservations(managerToken);

        // then
        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().get("memberName")).isEqualTo("브라운");
    }

    @Test
    @DisplayName("관리자는 관리자 예약 조회에서 모든 예약을 볼 수 있다")
    void adminFindsAllReservations() {
        // given
        createMember("어드민", "admin@example.com", Role.ADMIN);
        createMember("코니", "cony@example.com", Role.USER);
        String adminToken = login("admin@example.com");
        String userToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        createReservation(adminToken, LocalDate.of(2030, 5, 1), tenOClockId, themeId);
        createReservation(userToken, LocalDate.of(2030, 5, 2), elevenOClockId, themeId);

        // when
        List<Map<String, Object>> reservations = findAdminReservations(adminToken);

        // then
        assertThat(reservations)
                .extracting(reservation -> reservation.get("memberName"))
                .containsExactly("어드민", "코니");
    }
}

package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.repository.MemberRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationAuthorizationHttpIntegrationTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("매니저는 관리 예약 목록에서 자신의 예약만 조회한다")
    void managerFindsOnlyOwnManageableReservations() {
        memberRepository.save(new Member("매니저", "manager@example.com", "password", Role.MANAGER));
        createMember("브라운", "brown@example.com");
        String managerToken = login("manager@example.com");
        String brownToken = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        long managerReservationId = ((Number) createReservation(
                managerToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();
        long brownReservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                elevenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<List> response = findManageableReservations(managerToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(item -> ((Number) ((Map<String, Object>) item).get("id")).longValue())
                .containsExactly(managerReservationId)
                .doesNotContain(brownReservationId);
    }

    @Test
    @DisplayName("관리자는 모든 예약을 조회한다")
    void adminFindsAllReservations() {
        memberRepository.save(new Member("관리자", "admin@example.com", "password", Role.ADMIN));
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String adminToken = login("admin@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        long brownReservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();
        long conyReservationId = ((Number) createReservation(
                conyToken,
                LocalDate.of(2030, 5, 1),
                elevenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<List> response = findManageableReservations(adminToken);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(item -> ((Number) ((Map<String, Object>) item).get("id")).longValue())
                .containsExactly(brownReservationId, conyReservationId);
    }

    @Test
    @DisplayName("일반 회원은 관리 예약 목록을 조회할 수 없다")
    void userCannotFindManageableReservations() {
        createMember("브라운", "brown@example.com");
        String brownToken = login("brown@example.com");

        ResponseEntity<Map> response = restTemplate.exchange(
                "/admin/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(brownToken)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }
}

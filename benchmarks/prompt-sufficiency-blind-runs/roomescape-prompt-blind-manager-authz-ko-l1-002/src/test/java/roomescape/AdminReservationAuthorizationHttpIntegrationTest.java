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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.auth.AuthTokenProvider;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.repository.MemberRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminReservationAuthorizationHttpIntegrationTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthTokenProvider authTokenProvider;

    @Test
    @DisplayName("매니저는 관리자 예약 목록에서 자신의 예약만 조회한다")
    void managerFindsOnlyOwnReservationsFromAdminReservationList() {
        // given
        Member manager = memberRepository.save(new Member("매니저", "manager@example.com", "password", Role.MANAGER));
        Member other = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        String managerToken = authTokenProvider.createToken(manager);
        String otherToken = authTokenProvider.createToken(other);
        long themeId = createTheme("어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);

        long managerReservationId = ((Number) createReservation(managerToken, date, tenOClockId, themeId)
                .getBody()
                .get("id")).longValue();
        long otherReservationId = ((Number) createReservation(otherToken, date, elevenOClockId, themeId)
                .getBody()
                .get("id")).longValue();

        // when
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                "/admin/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(managerToken)),
                new ParameterizedTypeReference<>() {
                }
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(reservation -> ((Number) reservation.get("id")).longValue())
                .containsExactly(managerReservationId)
                .doesNotContain(otherReservationId);
    }

    @Test
    @DisplayName("일반 회원은 관리자 예약 목록을 조회할 수 없다")
    void userCannotFindAdminReservationList() {
        Member user = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        String userToken = authTokenProvider.createToken(user);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "/admin/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }
}

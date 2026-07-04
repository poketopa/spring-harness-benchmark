package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.domain.Theme;
import roomescape.repository.MemberRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationHttpIntegrationTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Test
    @DisplayName("다른 회원의 예약을 변경하면 찾을 수 없음 응답을 반환한다")
    void changeOtherMemberReservationReturnsNotFound() {
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = createTheme("어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        long reservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<Map> response = changeReservation(
                conyToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("RESERVATION_NOT_FOUND");
    }

    @Test
    @DisplayName("매장 매니저는 자기 매장 예약을 변경할 수 있다")
    void managerChangesOwnThemeReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        createMember("브라운", "brown@example.com");
        String managerToken = login(manager.getEmail());
        String brownToken = login("brown@example.com");
        long themeId = saveTheme("매니저 방", manager);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        long reservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<Map> response = changeReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("date")).isEqualTo("2030-05-02");
        assertThat(((Number) response.getBody().get("timeId")).longValue()).isEqualTo(elevenOClockId);
    }

    @Test
    @DisplayName("매장 매니저가 다른 매장 예약을 변경하면 찾을 수 없음 응답을 반환한다")
    void managerChangingOtherThemeReservationReturnsNotFound() {
        Member ownManager = saveManager("매니저", "manager@example.com");
        Member otherManager = saveManager("다른매니저", "other-manager@example.com");
        createMember("브라운", "brown@example.com");
        String managerToken = login(ownManager.getEmail());
        String brownToken = login("brown@example.com");
        long otherThemeId = saveTheme("다른 매장 방", otherManager);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        long reservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                otherThemeId
        ).getBody().get("id")).longValue();

        ResponseEntity<Map> response = changeReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("RESERVATION_NOT_FOUND");
    }

    @Test
    @DisplayName("일반 회원이 다른 회원의 매장 예약을 취소하면 찾을 수 없음 응답을 반환한다")
    void userCancelingOtherThemeReservationReturnsNotFound() {
        Member manager = saveManager("매니저", "manager@example.com");
        createMember("브라운", "brown@example.com");
        createMember("코니", "cony@example.com");
        String brownToken = login("brown@example.com");
        String conyToken = login("cony@example.com");
        long themeId = saveTheme("매니저 방", manager);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long reservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<Void> response = cancelReservation(conyToken, reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("매장 매니저는 자기 매장 예약을 취소할 수 있다")
    void managerCancelsOwnThemeReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        createMember("브라운", "brown@example.com");
        String managerToken = login(manager.getEmail());
        String brownToken = login("brown@example.com");
        long themeId = saveTheme("매니저 방", manager);
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long reservationId = ((Number) createReservation(
                brownToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        ResponseEntity<Void> response = cancelReservation(managerToken, reservationId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(brownToken)).isEmpty();
    }

    private Member saveManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }

    private long saveTheme(String name, Member manager) {
        Theme theme = themeRepository.save(new Theme(
                name,
                "방탈출",
                "https://example.com/" + name + ".jpg",
                manager
        ));
        return theme.getId();
    }
}

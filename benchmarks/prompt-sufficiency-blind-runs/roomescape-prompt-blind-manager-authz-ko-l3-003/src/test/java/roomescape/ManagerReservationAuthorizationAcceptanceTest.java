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
import roomescape.repository.MemberRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationAuthorizationAcceptanceTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("매장 매니저는 담당 매장의 예약 날짜와 시간을 변경할 수 있다")
    void managerChangesOwnStoreReservation() {
        // given
        Member manager = createManager("매니저", "manager@example.com");
        createMember("브라운", "brown@example.com");
        String managerToken = login("manager@example.com");
        String memberToken = login("brown@example.com");
        long themeId = createTheme("어둠의 방", manager.getId());
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        LocalDate originalDate = LocalDate.of(2030, 5, 1);
        LocalDate changedDate = LocalDate.of(2030, 5, 2);
        long reservationId = ((Number) createReservation(memberToken, originalDate, tenOClockId, themeId)
                .getBody()
                .get("id")).longValue();

        // when
        ResponseEntity<Map> response = changeReservation(managerToken, reservationId, changedDate, elevenOClockId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("date")).isEqualTo(changedDate.toString());
        assertThat(response.getBody().get("timeId")).isEqualTo((int) elevenOClockId);
    }

    @Test
    @DisplayName("매장 매니저는 담당 매장의 예약을 취소할 수 있다")
    void managerCancelsOwnStoreReservation() {
        // given
        Member manager = createManager("매니저", "manager@example.com");
        createMember("브라운", "brown@example.com");
        String managerToken = login("manager@example.com");
        String memberToken = login("brown@example.com");
        long themeId = createTheme("어둠의 방", manager.getId());
        long timeId = createTime(LocalTime.of(10, 0));
        long reservationId = ((Number) createReservation(
                memberToken,
                LocalDate.of(2030, 5, 1),
                timeId,
                themeId
        ).getBody().get("id")).longValue();

        // when
        ResponseEntity<Void> response = cancelReservation(managerToken, reservationId);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(memberToken)).isEmpty();
    }

    @Test
    @DisplayName("매장 매니저는 다른 매장의 예약을 관리할 수 없다")
    void managerCannotChangeOtherStoreReservation() {
        // given
        Member ownerManager = createManager("담당 매니저", "owner@example.com");
        createManager("다른 매니저", "other@example.com");
        createMember("브라운", "brown@example.com");
        String otherManagerToken = login("other@example.com");
        String memberToken = login("brown@example.com");
        long themeId = createTheme("어둠의 방", ownerManager.getId());
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        long reservationId = ((Number) createReservation(
                memberToken,
                LocalDate.of(2030, 5, 1),
                tenOClockId,
                themeId
        ).getBody().get("id")).longValue();

        // when
        ResponseEntity<Map> response = changeReservation(
                otherManagerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("일반 회원은 다른 회원의 예약을 관리할 수 없다")
    void memberCannotChangeOtherMemberReservation() {
        // given
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

        // when
        ResponseEntity<Map> response = changeReservation(
                conyToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                elevenOClockId
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("RESERVATION_NOT_FOUND");
    }

    private Member createManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }
}

package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.dto.WaitingRequest;
import roomescape.dto.WaitingResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.service.WaitingService;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WaitingServiceIntegrationTest {

    @Autowired
    private WaitingService waitingService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("이미 예약된 슬롯에 대기를 생성하고 순번을 반환한다")
    void createWaitingForReservedSlot() {
        WaitingFixture fixture = saveReservedSlot();
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));

        WaitingResponse response = waitingService.create(
                new LoginMember(cony.getId(), cony.getName()),
                new WaitingRequest(fixture.date(), fixture.time().getId(), fixture.theme().getId())
        );

        assertThat(response.memberId()).isEqualTo(cony.getId());
        assertThat(response.waitingRank()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 회원이 같은 슬롯에 중복 대기하면 예외가 발생한다")
    void duplicateWaitingThrowsException() {
        WaitingFixture fixture = saveReservedSlot();
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        LoginMember loginMember = new LoginMember(cony.getId(), cony.getName());
        WaitingRequest request = new WaitingRequest(fixture.date(), fixture.time().getId(), fixture.theme().getId());
        waitingService.create(loginMember, request);

        assertThatThrownBy(() -> waitingService.create(loginMember, request))
                .isInstanceOf(RoomescapeException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DUPLICATE_WAITING);
    }

    private WaitingFixture saveReservedSlot() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);

        reservationRepository.save(new Reservation(brown, theme, time, date));

        return new WaitingFixture(theme, time, date);
    }

    private record WaitingFixture(Theme theme, ReservationTime time, LocalDate date) {
    }
}

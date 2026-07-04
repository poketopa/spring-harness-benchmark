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
    @DisplayName("예약된 슬롯에 대기를 생성하면 신청 순서 기준 순번을 반환한다")
    void createWaitingForReservedSlotReturnsRank() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        reservationRepository.save(new Reservation(brown, theme, time, date));

        WaitingResponse conyWaiting = waitingService.create(loginMember(cony), new WaitingRequest(date, time.getId(), theme.getId()));
        WaitingResponse sallyWaiting = waitingService.create(loginMember(sally), new WaitingRequest(date, time.getId(), theme.getId()));

        assertThat(conyWaiting.rank()).isEqualTo(1);
        assertThat(sallyWaiting.rank()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 회원이 같은 슬롯에 중복 대기하면 예외가 발생한다")
    void duplicateWaitingThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        WaitingRequest request = new WaitingRequest(date, time.getId(), theme.getId());
        reservationRepository.save(new Reservation(brown, theme, time, date));
        waitingService.create(loginMember(cony), request);

        assertThatThrownBy(() -> waitingService.create(loginMember(cony), request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_WAITING));
    }

    private Member saveMember(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

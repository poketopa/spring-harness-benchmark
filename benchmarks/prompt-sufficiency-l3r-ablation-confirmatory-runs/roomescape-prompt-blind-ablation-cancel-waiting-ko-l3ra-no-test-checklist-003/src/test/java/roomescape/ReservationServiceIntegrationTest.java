package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import roomescape.domain.Waiting;
import roomescape.dto.MyReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;
import roomescape.service.ReservationService;
import roomescape.service.WaitingService;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

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

    @Autowired
    private WaitingRepository waitingRepository;

    @Test
    @DisplayName("예약 취소 시 같은 슬롯의 가장 빠른 대기를 예약으로 승격하고 남은 대기 순번을 재계산한다")
    void cancelPromotesFirstWaitingAndRecalculatesRanks() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));
        waitingRepository.save(new Waiting(cony, theme, time, date, LocalDateTime.of(2029, 1, 1, 10, 0)));
        waitingRepository.save(new Waiting(sally, theme, time, date, LocalDateTime.of(2029, 1, 1, 10, 1)));

        reservationService.cancel(loginMember(brown), reservation.getId());

        Reservation promotedReservation = reservationRepository.findByThemeAndTimeAndDate(theme, time, date).orElseThrow();
        assertThat(promotedReservation.isOwnedBy(cony)).isTrue();
        assertThat(waitingRepository.findAll()).hasSize(1);

        MyReservationResponse sallyWaiting = waitingService.findMine(loginMember(sally)).getFirst();
        assertThat(sallyWaiting.rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 회원의 예약을 취소하면 예외가 발생한다")
    void cancelOtherMemberReservationThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        Reservation reservation = reservationRepository.save(
                new Reservation(brown, theme, time, LocalDate.of(2030, 5, 1))
        );

        assertThatThrownBy(() -> reservationService.cancel(loginMember(cony), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
    }

    @Test
    @DisplayName("지난 예약을 취소하면 예외가 발생한다")
    void cancelPastReservationThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        Reservation reservation = reservationRepository.save(
                new Reservation(brown, theme, time, LocalDate.of(2000, 5, 1))
        );

        assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAST_RESERVATION));
    }

    private Member saveMember(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;
import roomescape.service.ReservationService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @MockitoSpyBean
    private ReservationRepository reservationRepository;

    @Autowired
    private WaitingRepository waitingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Test
    @DisplayName("예약 승격 저장에 실패하면 예약 삭제와 대기 삭제를 함께 롤백한다")
    void promotionFailureRollsBackCancellation() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(brown, theme, time, date));
        Waiting waiting = waitingRepository.saveAndFlush(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));
        doThrow(new DataIntegrityViolationException("promotion failed"))
                .when(reservationRepository)
                .saveAndFlush(argThat(target -> isPromotionFor(target, cony)));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                .isInstanceOfSatisfying(DataIntegrityViolationException.class, exception ->
                        assertThat(exception.getMessage()).contains("promotion failed"));

        assertThat(reservationRepository.findById(reservation.getId())).isPresent();
        assertThat(waitingRepository.findById(waiting.getId())).isPresent();
    }

    @Test
    @DisplayName("다른 회원의 예약 취소는 기존 소유권 에러 정책에 따라 찾을 수 없음 예외가 발생한다")
    void cancelOtherMemberReservationThrowsNotFound() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        Reservation reservation = reservationRepository.save(new Reservation(
                brown,
                theme,
                time,
                LocalDate.of(2030, 5, 1)
        ));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(cony), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
    }

    @Test
    @DisplayName("지난 예약을 취소하면 지난 예약 예외가 발생한다")
    void cancelPastReservationThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        Reservation reservation = reservationRepository.save(new Reservation(
                brown,
                theme,
                time,
                LocalDate.of(2000, 5, 1)
        ));

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

    private boolean isPromotionFor(Reservation reservation, Member member) {
        return reservation != null && reservation.isOwnedBy(member);
    }
}

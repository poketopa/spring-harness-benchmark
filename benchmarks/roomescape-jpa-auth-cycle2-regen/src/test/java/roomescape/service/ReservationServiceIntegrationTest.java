package roomescape.service;

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
import roomescape.dto.ReservationChangeRequest;
import roomescape.dto.ReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Autowired
    private WaitingRepository waitingRepository;

    @Test
    @DisplayName("예약 날짜와 시간을 변경하면 변경된 예약 응답을 반환한다")
    void changeReservationDateAndTimeReturnsChangedReservation() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime tenOClock = saveTime(LocalTime.of(10, 0));
        ReservationTime elevenOClock = saveTime(LocalTime.of(11, 0));
        Reservation reservation = reservationRepository.save(new Reservation(
                brown,
                theme,
                tenOClock,
                LocalDate.of(2030, 5, 1)
        ));

        ReservationResponse response = reservationService.change(
                loginMember(brown),
                reservation.getId(),
                new ReservationChangeRequest(LocalDate.of(2030, 5, 2), elevenOClock.getId())
        );

        assertThat(response.date()).isEqualTo(LocalDate.of(2030, 5, 2));
        assertThat(response.timeId()).isEqualTo(elevenOClock.getId());
    }

    @Test
    @DisplayName("예약을 다른 슬롯으로 변경하면 기존 슬롯의 첫 대기가 예약으로 전환된다")
    void changeReservationPromotesFirstWaitingInPreviousSlot() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = saveTheme();
        ReservationTime tenOClock = saveTime(LocalTime.of(10, 0));
        ReservationTime elevenOClock = saveTime(LocalTime.of(11, 0));
        LocalDate originalDate = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(
                brown,
                theme,
                tenOClock,
                originalDate
        ));
        Waiting waiting = waitingRepository.save(new Waiting(
                cony,
                theme,
                tenOClock,
                originalDate,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));

        reservationService.change(
                loginMember(brown),
                reservation.getId(),
                new ReservationChangeRequest(LocalDate.of(2030, 5, 2), elevenOClock.getId())
        );

        Reservation promoted = reservationRepository.findByThemeAndTimeAndDate(theme, tenOClock, originalDate)
                .orElseThrow();
        assertThat(promoted.getMember().getId()).isEqualTo(cony.getId());
        assertThat(waitingRepository.findById(waiting.getId())).isEmpty();
    }

    @Test
    @DisplayName("지난 예약을 취소하면 예외가 발생한다")
    void cancelPastReservationThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime(LocalTime.of(10, 0));
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

    @Test
    @DisplayName("예약을 취소하면 첫 대기가 예약으로 전환되고 다음 대기 순번이 앞당겨진다")
    void cancelReservationPromotesFirstWaitingAndReordersNextWaiting() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));
        Waiting firstWaiting = waitingRepository.save(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));
        Waiting secondWaiting = waitingRepository.save(new Waiting(
                sally,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 1)
        ));

        reservationService.cancel(loginMember(brown), reservation.getId());

        Reservation promoted = reservationRepository.findByThemeAndTimeAndDate(theme, time, date)
                .orElseThrow();
        Waiting remainingWaiting = waitingRepository.findById(secondWaiting.getId()).orElseThrow();
        long earlierWaitingCount = waitingRepository.countEarlierWaitings(
                theme,
                time,
                date,
                remainingWaiting.getCreatedAt(),
                remainingWaiting.getId()
        );

        assertThat(promoted.getMember().getId()).isEqualTo(cony.getId());
        assertThat(waitingRepository.findById(firstWaiting.getId())).isEmpty();
        assertThat(earlierWaitingCount).isZero();
    }

    @Test
    @DisplayName("지난 날짜와 시간으로 예약을 변경하면 예외가 발생한다")
    void changeReservationToPastScheduleThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime(LocalTime.of(10, 0));
        Reservation reservation = reservationRepository.save(new Reservation(
                brown,
                theme,
                time,
                LocalDate.of(2030, 5, 1)
        ));

        assertThatThrownBy(() -> reservationService.change(
                loginMember(brown),
                reservation.getId(),
                new ReservationChangeRequest(LocalDate.of(2000, 5, 1), time.getId())
        ))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAST_RESERVATION));
    }

    @Test
    @DisplayName("다른 예약이 차지한 슬롯으로 변경하면 예외가 발생한다")
    void changeReservationToOccupiedSlotThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = saveTheme();
        ReservationTime tenOClock = saveTime(LocalTime.of(10, 0));
        ReservationTime elevenOClock = saveTime(LocalTime.of(11, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, tenOClock, date));
        reservationRepository.save(new Reservation(cony, theme, elevenOClock, date));

        assertThatThrownBy(() -> reservationService.change(
                loginMember(brown),
                reservation.getId(),
                new ReservationChangeRequest(date, elevenOClock.getId())
        ))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESERVATION));
    }

    private Member saveMember(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private Theme saveTheme() {
        return themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
    }

    private ReservationTime saveTime(LocalTime startAt) {
        return timeRepository.save(new ReservationTime(startAt));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

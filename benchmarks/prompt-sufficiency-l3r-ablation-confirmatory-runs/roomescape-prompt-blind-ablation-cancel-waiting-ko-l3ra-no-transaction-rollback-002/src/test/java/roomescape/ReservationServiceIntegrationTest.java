package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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
    @DisplayName("회원은 본인의 예약을 취소할 수 있다")
    void memberCancelsOwnReservation() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        Reservation reservation = saveReservation(brown, theme, time, LocalDate.of(2030, 5, 1));

        reservationService.cancel(loginMember(brown), reservation.getId());

        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("지난 예약을 취소하면 예외가 발생한다")
    void pastReservationCancelThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        Reservation reservation = saveReservation(brown, theme, time, LocalDate.of(2000, 5, 1));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAST_RESERVATION));
        assertThat(reservationRepository.findById(reservation.getId())).isPresent();
    }

    @Test
    @DisplayName("다른 회원의 예약을 취소하면 찾을 수 없음 예외가 발생한다")
    void otherMemberReservationCancelThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        Reservation reservation = saveReservation(brown, theme, time, LocalDate.of(2030, 5, 1));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(cony), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        assertThat(reservationRepository.findById(reservation.getId())).isPresent();
    }

    @Test
    @DisplayName("예약을 취소하면 같은 슬롯의 가장 빠른 대기자가 예약으로 승격된다")
    void cancelReservationPromotesFirstWaiting() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = saveReservation(brown, theme, time, date);
        Waiting firstWaiting = saveWaiting(cony, theme, time, date, LocalDateTime.of(2030, 1, 1, 10, 0));
        Waiting secondWaiting = saveWaiting(sally, theme, time, date, LocalDateTime.of(2030, 1, 1, 10, 1));

        reservationService.cancel(loginMember(brown), reservation.getId());

        Reservation promotedReservation = reservationRepository.findByThemeAndTimeAndDate(theme, time, date).orElseThrow();
        assertThat(promotedReservation.getMember().getId()).isEqualTo(cony.getId());
        assertThat(waitingRepository.findById(firstWaiting.getId())).isEmpty();
        assertThat(waitingRepository.findById(secondWaiting.getId())).isPresent();
    }

    @Test
    @DisplayName("대기자가 예약으로 승격되면 남은 대기 순번을 신청 순서 기준으로 다시 계산한다")
    void cancelReservationRecalculatesRemainingWaitingRanks() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Member eric = saveMember("에릭", "eric@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = saveReservation(brown, theme, time, date);
        saveWaiting(cony, theme, time, date, LocalDateTime.of(2030, 1, 1, 10, 0));
        saveWaiting(sally, theme, time, date, LocalDateTime.of(2030, 1, 1, 10, 1));
        saveWaiting(eric, theme, time, date, LocalDateTime.of(2030, 1, 1, 10, 2));

        reservationService.cancel(loginMember(brown), reservation.getId());

        List<MyReservationResponse> sallyReservations = waitingService.findMine(loginMember(sally));
        List<MyReservationResponse> ericReservations = waitingService.findMine(loginMember(eric));
        assertThat(sallyReservations).singleElement().extracting(MyReservationResponse::rank).isEqualTo(1);
        assertThat(ericReservations).singleElement().extracting(MyReservationResponse::rank).isEqualTo(2);
    }

    private Member saveMember(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private Theme saveTheme() {
        return themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
    }

    private ReservationTime saveTime() {
        return timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
    }

    private Reservation saveReservation(Member member, Theme theme, ReservationTime time, LocalDate date) {
        return reservationRepository.saveAndFlush(new Reservation(member, theme, time, date));
    }

    private Waiting saveWaiting(
            Member member,
            Theme theme,
            ReservationTime time,
            LocalDate date,
            LocalDateTime createdAt
    ) {
        return waitingRepository.saveAndFlush(new Waiting(member, theme, time, date, createdAt));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
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
import roomescape.service.WaitingService;

@SpringBootTest
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("본인의 미래 예약을 취소하면 예약이 삭제된다")
    void cancelOwnReservationDeletesReservation() {
        Member brown = saveMember("브라운", "brown@example.com");
        Reservation reservation = saveReservation(brown, LocalDate.of(2030, 5, 1));

        reservationService.cancel(loginMember(brown), reservation.getId());

        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("지난 예약을 취소하면 예외가 발생하고 예약은 유지된다")
    void cancelPastReservationThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Reservation reservation = saveReservation(brown, LocalDate.of(2000, 5, 1));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAST_RESERVATION));
        assertThat(reservationRepository.findById(reservation.getId())).isPresent();
    }

    @Test
    @DisplayName("다른 회원의 예약을 취소하면 소유권 정책에 따라 찾을 수 없음 예외가 발생한다")
    void cancelOtherMemberReservationThrowsNotFound() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Reservation reservation = saveReservation(brown, LocalDate.of(2030, 5, 1));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(cony), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        assertThat(reservationRepository.findById(reservation.getId())).isPresent();
    }

    @Test
    @DisplayName("예약 취소 시 같은 슬롯의 가장 빠른 대기가 예약으로 승격되고 남은 대기 순번이 다시 계산된다")
    void cancelReservationPromotesFirstWaitingAndRecalculatesRank() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(brown, theme, time, date));
        Waiting firstWaiting = waitingRepository.saveAndFlush(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));
        Waiting secondWaiting = waitingRepository.saveAndFlush(new Waiting(
                sally,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 1)
        ));

        reservationService.cancel(loginMember(brown), reservation.getId());

        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
        assertThat(waitingRepository.findById(firstWaiting.getId())).isEmpty();
        assertThat(reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(cony)).hasSize(1);
        assertThat(waitingRepository.findById(secondWaiting.getId())).isPresent();
        assertThat(waitingService.findMine(loginMember(sally)).getFirst().rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("예약 승격이 실패하면 예약 삭제와 대기 삭제가 모두 롤백된다")
    void promotionFailureRollsBackReservationAndWaitingDeletion() {
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
        jdbcTemplate.execute("""
                create trigger fail_reservation_insert
                before insert on reservation
                for each row call 'roomescape.ReservationInsertFailTrigger'
                """);

        assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                .isInstanceOf(RuntimeException.class);

        assertThat(reservationRepository.findById(reservation.getId())).isPresent();
        assertThat(waitingRepository.findById(waiting.getId())).isPresent();
        assertThat(reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(cony)).isEmpty();
    }

    private Member saveMember(String name, String email) {
        return memberRepository.saveAndFlush(new Member(name, email, "password"));
    }

    private Reservation saveReservation(Member member, LocalDate date) {
        Theme theme = themeRepository.saveAndFlush(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.saveAndFlush(new ReservationTime(LocalTime.of(10, 0)));
        return reservationRepository.saveAndFlush(new Reservation(member, theme, time, date));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

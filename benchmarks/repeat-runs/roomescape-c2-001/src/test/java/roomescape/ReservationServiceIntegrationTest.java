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
import roomescape.dto.ReservationChangeRequest;
import roomescape.dto.ReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;
import roomescape.service.ReservationService;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

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
    @DisplayName("회원은 본인 예약 날짜와 시간을 변경한다")
    void changeOwnReservation() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime oldTime = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        ReservationTime newTime = timeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, oldTime, LocalDate.of(2030, 5, 1)));

        ReservationResponse response = reservationService.change(
                loginMember(brown),
                reservation.getId(),
                new ReservationChangeRequest(LocalDate.of(2030, 5, 2), newTime.getId())
        );

        assertThat(response.date()).isEqualTo(LocalDate.of(2030, 5, 2));
        assertThat(response.timeId()).isEqualTo(newTime.getId());
    }

    @Test
    @DisplayName("이미 예약된 슬롯으로 변경하면 예외가 발생한다")
    void changeToDuplicateSlotThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = saveTheme();
        ReservationTime oldTime = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        ReservationTime newTime = timeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, oldTime, LocalDate.of(2030, 5, 1)));
        reservationRepository.save(new Reservation(cony, theme, newTime, LocalDate.of(2030, 5, 2)));

        assertThatThrownBy(() -> reservationService.change(
                loginMember(brown),
                reservation.getId(),
                new ReservationChangeRequest(LocalDate.of(2030, 5, 2), newTime.getId())
        )).isInstanceOfSatisfying(RoomescapeException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESERVATION));
    }

    @Test
    @DisplayName("예약을 취소하면 첫 번째 대기가 예약으로 승격된다")
    void cancelReservationPromotesFirstWaiting() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Theme theme = saveTheme();
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));
        waitingRepository.save(new Waiting(cony, theme, time, date, LocalDateTime.of(2030, 1, 1, 0, 0)));
        waitingRepository.save(new Waiting(sally, theme, time, date, LocalDateTime.of(2030, 1, 1, 0, 1)));

        reservationService.cancel(loginMember(brown), reservation.getId());

        Reservation promoted = reservationRepository.findByThemeAndTimeAndDate(theme, time, date).orElseThrow();
        List<Waiting> remaining = waitingRepository.findAllByMemberOrderByDateAscTimeStartAtAscCreatedAtAsc(sally);
        assertThat(promoted.getMember().getId()).isEqualTo(cony.getId());
        assertThat(remaining).hasSize(1);
    }

    private Member saveMember(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private Theme saveTheme() {
        return themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

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

@SpringBootTest
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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("지난 예약은 취소할 수 없다")
    void pastReservationCancelThrowsException() {
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
        assertThat(reservationRepository.findById(reservation.getId())).isPresent();
    }

    @Test
    @DisplayName("대기 승격 실패 시 예약 삭제와 대기 삭제를 함께 롤백한다")
    void promotionFailureRollsBackReservationAndWaitingDeletion() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(brown, theme, time, date));
        Waiting waiting = waitingRepository.saveAndFlush(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));
        jdbcTemplate.execute("alter table reservation add constraint ck_block_promotion check (member_id <> "
                + cony.getId() + ")");

        try {
            assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                    .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESERVATION));
        } finally {
            jdbcTemplate.execute("alter table reservation drop constraint ck_block_promotion");
        }

        assertThat(reservationRepository.findById(reservation.getId())).isPresent();
        assertThat(waitingRepository.findById(waiting.getId())).isPresent();
        List<Reservation> reservations = reservationRepository.findAllByThemeAndDate(theme, date);
        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getMember().getId()).isEqualTo(brown.getId());
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

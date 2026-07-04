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

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WaitingPromotionServiceIntegrationTest {

    @Autowired
    private WaitingPromotionService waitingPromotionService;

    @Autowired
    private WaitingRepository waitingRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Test
    @DisplayName("첫 대기를 예약으로 승격한다")
    void promoteFirstWaitingCreatesReservation() {
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Waiting waiting = waitingRepository.save(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));

        waitingPromotionService.promoteFirstWaiting(theme, time, date);

        assertThat(waitingRepository.findById(waiting.getId())).isEmpty();
        assertThat(reservationRepository.findByThemeAndTimeAndDate(theme, time, date)).isPresent();
    }

    @Test
    @DisplayName("승격 중 예약 생성이 실패하면 대기 삭제도 롤백한다")
    void promotionFailureRollsBackWaitingDelete() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        reservationRepository.save(new Reservation(brown, theme, time, date));
        Waiting waiting = waitingRepository.save(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));

        assertThatThrownBy(() -> waitingPromotionService.promoteFirstWaiting(theme, time, date))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESERVATION));

        assertThat(waitingRepository.findById(waiting.getId())).isPresent();
    }
}

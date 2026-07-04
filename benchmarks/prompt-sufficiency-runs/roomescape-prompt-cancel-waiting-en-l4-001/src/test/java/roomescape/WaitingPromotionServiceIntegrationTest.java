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
import org.springframework.transaction.support.TransactionTemplate;
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
import roomescape.service.WaitingPromotionService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WaitingPromotionServiceIntegrationTest {

    @Autowired
    private WaitingPromotionService waitingPromotionService;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
    @DisplayName("대기 승격 실패 시 삭제한 대기를 롤백한다")
    void promotionFailureRollsBackWaitingDeletion() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(brown, theme, time, date));
        waitingRepository.saveAndFlush(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));

        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            Reservation existingReservation = reservationRepository.findById(reservation.getId()).orElseThrow();
            waitingPromotionService.promoteFirstWaiting(existingReservation);
        })).isInstanceOfSatisfying(RoomescapeException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESERVATION));

        assertThat(reservationRepository.findAll()).hasSize(1);
        assertThat(waitingRepository.findAll()).hasSize(1);
    }
}

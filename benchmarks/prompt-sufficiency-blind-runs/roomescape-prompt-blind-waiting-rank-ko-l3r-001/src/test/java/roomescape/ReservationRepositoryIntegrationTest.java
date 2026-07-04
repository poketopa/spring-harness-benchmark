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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.ReservationWaiting;
import roomescape.domain.Theme;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ReservationWaitingRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationRepositoryIntegrationTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ReservationWaitingRepository reservationWaitingRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Test
    @DisplayName("같은 슬롯에 두 예약을 저장하면 DB 제약조건으로 실패한다")
    void duplicateSlotFailsByDatabaseConstraint() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        reservationRepository.saveAndFlush(new Reservation(brown, theme, time, date));

        Reservation duplicate = new Reservation(cony, theme, time, date);

        assertThatThrownBy(() -> reservationRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 회원의 같은 슬롯 대기를 저장하면 DB 제약조건으로 실패한다")
    void duplicateWaitingFailsByDatabaseConstraint() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        LocalDateTime createdAt = LocalDateTime.of(2030, 1, 1, 12, 0);
        reservationRepository.saveAndFlush(new Reservation(brown, theme, time, date));
        reservationWaitingRepository.saveAndFlush(new ReservationWaiting(cony, theme, time, date, createdAt));

        ReservationWaiting duplicate = new ReservationWaiting(cony, theme, time, date, createdAt.plusSeconds(1));

        assertThatThrownBy(() -> reservationWaitingRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("대기 신청 시간이 같으면 ID 오름차순으로 순서를 결정한다")
    void waitingOrderUsesIdAsTieBreaker() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Member sally = memberRepository.save(new Member("샐리", "sally@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        LocalDateTime createdAt = LocalDateTime.of(2030, 1, 1, 12, 0);
        reservationRepository.saveAndFlush(new Reservation(brown, theme, time, date));
        ReservationWaiting first = reservationWaitingRepository.saveAndFlush(
                new ReservationWaiting(cony, theme, time, date, createdAt)
        );
        ReservationWaiting second = reservationWaitingRepository.saveAndFlush(
                new ReservationWaiting(sally, theme, time, date, createdAt)
        );

        assertThat(reservationWaitingRepository.findAllByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(theme, time, date))
                .extracting(ReservationWaiting::getId)
                .containsExactly(first.getId(), second.getId());
    }
}

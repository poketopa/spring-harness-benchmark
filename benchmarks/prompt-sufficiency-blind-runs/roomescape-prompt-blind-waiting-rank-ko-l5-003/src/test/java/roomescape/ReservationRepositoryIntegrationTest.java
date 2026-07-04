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
    @DisplayName("같은 회원이 같은 슬롯에 두 대기를 저장하면 DB 제약조건으로 실패한다")
    void duplicateWaitingSlotFailsByDatabaseConstraint() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        reservationWaitingRepository.saveAndFlush(new ReservationWaiting(
                brown,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 0, 0)
        ));

        ReservationWaiting duplicate = new ReservationWaiting(
                brown,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 0, 1)
        );

        assertThatThrownBy(() -> reservationWaitingRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("대기 생성 시각이 같으면 식별자 순서로 순번을 계산한다")
    void waitingRankUsesIdAsTieBreaker() {
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Member sally = memberRepository.save(new Member("샐리", "sally@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        LocalDateTime createdAt = LocalDateTime.of(2030, 1, 1, 0, 0);
        ReservationWaiting first = reservationWaitingRepository.saveAndFlush(new ReservationWaiting(
                cony,
                theme,
                time,
                date,
                createdAt
        ));
        ReservationWaiting second = reservationWaitingRepository.saveAndFlush(new ReservationWaiting(
                sally,
                theme,
                time,
                date,
                createdAt
        ));

        long aheadOfFirst = reservationWaitingRepository.countAhead(
                theme,
                time,
                date,
                first.getCreatedAt(),
                first.getId()
        );
        long aheadOfSecond = reservationWaitingRepository.countAhead(
                theme,
                time,
                date,
                second.getCreatedAt(),
                second.getId()
        );

        assertThat(aheadOfFirst + 1).isEqualTo(1);
        assertThat(aheadOfSecond + 1).isEqualTo(2);
    }
}

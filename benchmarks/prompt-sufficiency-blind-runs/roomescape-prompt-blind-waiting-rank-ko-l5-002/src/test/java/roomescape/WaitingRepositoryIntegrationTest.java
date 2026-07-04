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
import roomescape.domain.Theme;
import roomescape.domain.Waiting;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class WaitingRepositoryIntegrationTest {

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
    @DisplayName("같은 회원이 같은 예약에 두 대기를 저장하면 DB 제약조건으로 실패한다")
    void duplicateWaitingFailsByDatabaseConstraint() {
        Reservation reservation = createReservation();
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        waitingRepository.saveAndFlush(new Waiting(cony, reservation, LocalDateTime.of(2030, 1, 1, 1, 0)));

        Waiting duplicate = new Waiting(cony, reservation, LocalDateTime.of(2030, 1, 1, 1, 1));

        assertThatThrownBy(() -> waitingRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("대기 순번은 신청 시각이 같으면 id 순서로 결정된다")
    void rankUsesIdTieBreaker() {
        Reservation reservation = createReservation();
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Member sally = memberRepository.save(new Member("샐리", "sally@example.com", "password"));
        LocalDateTime sameTime = LocalDateTime.of(2030, 1, 1, 1, 0);
        Waiting first = waitingRepository.saveAndFlush(new Waiting(cony, reservation, sameTime));
        Waiting second = waitingRepository.saveAndFlush(new Waiting(sally, reservation, sameTime));

        assertThat(waitingRepository.rankOf(reservation, first.getCreatedAt(), first.getId())).isEqualTo(1);
        assertThat(waitingRepository.rankOf(reservation, second.getCreatedAt(), second.getId())).isEqualTo(2);
    }

    private Reservation createReservation() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        return reservationRepository.saveAndFlush(new Reservation(brown, theme, time, LocalDate.of(2030, 5, 1)));
    }
}

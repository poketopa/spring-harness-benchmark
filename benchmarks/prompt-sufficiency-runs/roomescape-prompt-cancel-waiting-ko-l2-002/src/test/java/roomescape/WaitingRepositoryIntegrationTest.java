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
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;
import roomescape.repository.MemberRepository;
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
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Test
    @DisplayName("같은 회원이 같은 슬롯에 두 대기를 저장하면 DB 제약조건으로 실패한다")
    void duplicateWaitingFailsByDatabaseConstraint() {
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        waitingRepository.saveAndFlush(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));

        Waiting duplicate = new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 1)
        );

        assertThatThrownBy(() -> waitingRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("같은 슬롯에서 대상 대기보다 앞선 대기 수를 조회한다")
    void countEarlierWaitingsReturnsPreviousWaitingCount() {
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Member sally = memberRepository.save(new Member("샐리", "sally@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        waitingRepository.saveAndFlush(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));
        Waiting second = waitingRepository.saveAndFlush(new Waiting(
                sally,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 1)
        ));

        long earlierWaitingCount = waitingRepository.countEarlierWaitings(
                theme,
                time,
                date,
                second.getCreatedAt(),
                second.getId()
        );

        assertThat(earlierWaitingCount).isEqualTo(1);
    }
}

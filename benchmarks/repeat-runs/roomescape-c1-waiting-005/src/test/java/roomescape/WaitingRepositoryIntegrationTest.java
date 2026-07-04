package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
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
    @DisplayName("같은 회원이 같은 슬롯에 두 대기를 저장하면 DB 제약조건으로 실패한다")
    void duplicateMemberSlotFailsByDatabaseConstraint() {
        WaitingFixture fixture = prepareReservedSlot();
        waitingRepository.saveAndFlush(new Waiting(fixture.waitingMember(), fixture.theme(), fixture.time(), fixture.date()));

        Waiting duplicate = new Waiting(fixture.waitingMember(), fixture.theme(), fixture.time(), fixture.date());

        assertThatThrownBy(() -> waitingRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("대기 순번 계산에 필요한 앞선 대기 수를 조회한다")
    void countEarlierWaitingsForRank() {
        WaitingFixture fixture = prepareReservedSlot();
        Waiting first = waitingRepository.saveAndFlush(
                new Waiting(fixture.waitingMember(), fixture.theme(), fixture.time(), fixture.date())
        );
        Member secondMember = memberRepository.save(new Member("어피치", "apeach@example.com", "password"));
        Waiting second = waitingRepository.saveAndFlush(
                new Waiting(secondMember, fixture.theme(), fixture.time(), fixture.date())
        );

        long earlierCount = waitingRepository.countByThemeAndTimeAndDateAndIdLessThan(
                fixture.theme(),
                fixture.time(),
                fixture.date(),
                second.getId()
        );

        assertThat(earlierCount).isEqualTo(1);
        assertThat(first.getId()).isLessThan(second.getId());
    }

    private WaitingFixture prepareReservedSlot() {
        Member reservedMember = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Member waitingMember = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        reservationRepository.saveAndFlush(new Reservation(reservedMember, theme, time, date));
        return new WaitingFixture(waitingMember, theme, time, date);
    }

    private record WaitingFixture(Member waitingMember, Theme theme, ReservationTime time, LocalDate date) {
    }
}

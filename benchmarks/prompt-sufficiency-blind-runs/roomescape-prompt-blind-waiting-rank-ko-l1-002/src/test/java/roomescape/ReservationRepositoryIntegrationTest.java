package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationStatus;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationRepositoryIntegrationTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Test
    @DisplayName("같은 슬롯에 확정 예약과 대기 예약을 함께 저장할 수 있다")
    void sameSlotCanHaveConfirmedAndWaitingReservations() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        reservationRepository.saveAndFlush(new Reservation(brown, theme, time, date, ReservationStatus.CONFIRMED));

        Reservation waiting = new Reservation(cony, theme, time, date, ReservationStatus.WAITING);

        Reservation savedWaiting = reservationRepository.saveAndFlush(waiting);

        assertThat(savedWaiting.getId()).isNotNull();
        assertThat(reservationRepository.findAll()).hasSize(2);
    }
}

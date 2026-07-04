package roomescape.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;

@DataJpaTest
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
    @DisplayName("변경 대상 예약 자신은 제외하고 같은 슬롯 예약 존재 여부를 조회한다")
    void existsDuplicateSlotExcludingSelf() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Member cony = memberRepository.save(new Member("코니", "cony@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime tenOClock = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        ReservationTime elevenOClock = timeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation brownReservation = reservationRepository.saveAndFlush(new Reservation(brown, theme, tenOClock, date));
        reservationRepository.saveAndFlush(new Reservation(cony, theme, elevenOClock, date));

        boolean sameReservationExists = reservationRepository.existsByThemeAndTimeAndDateAndIdNot(
                theme,
                tenOClock,
                date,
                brownReservation.getId()
        );
        boolean otherReservationExists = reservationRepository.existsByThemeAndTimeAndDateAndIdNot(
                theme,
                elevenOClock,
                date,
                brownReservation.getId()
        );

        assertThat(sameReservationExists).isFalse();
        assertThat(otherReservationExists).isTrue();
    }

    @Test
    @DisplayName("테마와 날짜로 예약된 시간 목록을 조회한다")
    void findReservationsByThemeAndDate() {
        Member brown = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));

        List<Reservation> found = reservationRepository.findAllByThemeAndDate(theme, date);

        assertThat(found).extracting(Reservation::getId).containsExactly(reservation.getId());
    }
}

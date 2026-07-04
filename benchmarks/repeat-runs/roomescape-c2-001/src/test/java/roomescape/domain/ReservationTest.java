package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReservationTest {

    @Test
    @DisplayName("현재 시각보다 이전 예약인지 판단한다")
    void pastReservationIsDetectedByNow() {
        Member member = new Member("브라운", "brown@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        Reservation reservation = new Reservation(member, theme, time, LocalDate.of(2030, 5, 1));

        boolean past = reservation.isPast(LocalDateTime.of(2030, 5, 1, 10, 1));

        assertThat(past).isTrue();
    }

    @Test
    @DisplayName("예약 날짜와 시간을 변경한다")
    void changeSchedule() {
        Member member = new Member("브라운", "brown@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime oldTime = new ReservationTime(LocalTime.of(10, 0));
        ReservationTime newTime = new ReservationTime(LocalTime.of(11, 0));
        Reservation reservation = new Reservation(member, theme, oldTime, LocalDate.of(2030, 5, 1));

        reservation.changeSchedule(newTime, LocalDate.of(2030, 5, 2));

        assertThat(reservation.getTime()).isEqualTo(newTime);
        assertThat(reservation.getDate()).isEqualTo(LocalDate.of(2030, 5, 2));
    }
}

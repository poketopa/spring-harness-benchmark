package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WaitingTest {

    @Test
    @DisplayName("현재 시각보다 이전 대기 대상인지 판단한다")
    void pastWaitingIsDetectedByNow() {
        Member member = new Member("코니", "cony@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        Waiting waiting = new Waiting(
                member,
                theme,
                time,
                LocalDate.of(2030, 5, 1),
                LocalDateTime.of(2030, 1, 1, 10, 0)
        );

        boolean past = waiting.isPast(LocalDateTime.of(2030, 5, 1, 10, 1));

        assertThat(past).isTrue();
    }

    @Test
    @DisplayName("대기 소유 회원인지 판단한다")
    void ownedWaitingIsDetectedByMember() {
        Member member = new Member("코니", "cony@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        Waiting waiting = new Waiting(
                member,
                theme,
                time,
                LocalDate.of(2030, 5, 1),
                LocalDateTime.of(2030, 1, 1, 10, 0)
        );

        assertThat(waiting.isOwnedBy(member)).isTrue();
    }

    @Test
    @DisplayName("대기를 예약으로 승인한다")
    void approveWaitingToReservation() {
        Member member = new Member("코니", "cony@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        Waiting waiting = new Waiting(
                member,
                theme,
                time,
                LocalDate.of(2030, 5, 1),
                LocalDateTime.of(2030, 1, 1, 10, 0)
        );

        Reservation reservation = waiting.approve();

        assertThat(reservation.getMember()).isEqualTo(member);
        assertThat(reservation.getTheme()).isEqualTo(theme);
        assertThat(reservation.getTime()).isEqualTo(time);
        assertThat(reservation.getDate()).isEqualTo(LocalDate.of(2030, 5, 1));
    }
}

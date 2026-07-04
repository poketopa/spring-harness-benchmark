package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class ReservationTest {

    @Test
    @DisplayName("현재 시각보다 이전 예약인지 판단한다")
    void pastReservationIsDetectedByNow() {
        Reservation reservation = new Reservation(member(), theme(), time(10), LocalDate.of(2030, 5, 1));

        boolean past = reservation.isPast(LocalDateTime.of(2030, 5, 1, 10, 1));

        assertThat(past).isTrue();
    }

    @Test
    @DisplayName("예약 날짜와 시간을 변경한다")
    void changeReservationSchedule() {
        ReservationTime elevenOClock = time(11);
        Reservation reservation = new Reservation(member(), theme(), time(10), LocalDate.of(2030, 5, 1));

        reservation.changeSchedule(LocalDate.of(2030, 5, 2), elevenOClock);

        assertThat(reservation.getDate()).isEqualTo(LocalDate.of(2030, 5, 2));
        assertThat(reservation.getTime()).isEqualTo(elevenOClock);
    }

    @Test
    @DisplayName("예약 변경 날짜와 시간은 비어 있을 수 없다")
    void nullScheduleIsRejectedWhenChangingReservation() {
        Reservation reservation = new Reservation(member(), theme(), time(10), LocalDate.of(2030, 5, 1));

        assertThatThrownBy(() -> reservation.changeSchedule(null, time(11)))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> reservation.changeSchedule(LocalDate.of(2030, 5, 2), null))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("예약이 같은 날짜와 시간인지 판단한다")
    void sameScheduleIsDetected() {
        ReservationTime time = time(10);
        Reservation reservation = new Reservation(member(), theme(), time, LocalDate.of(2030, 5, 1));

        assertThat(reservation.hasSchedule(LocalDate.of(2030, 5, 1), time)).isTrue();
    }

    @Test
    @DisplayName("예약이 다른 날짜와 시간인지 판단한다")
    void differentScheduleIsDetected() {
        Reservation reservation = new Reservation(member(), theme(), time(10), LocalDate.of(2030, 5, 1));

        assertThat(reservation.hasSchedule(LocalDate.of(2030, 5, 2), time(10))).isFalse();
        assertThat(reservation.hasSchedule(LocalDate.of(2030, 5, 1), time(11))).isFalse();
    }

    @Test
    @DisplayName("예약 회원, 테마, 시간, 날짜는 비어 있을 수 없다")
    void nullReservationFieldsAreRejected() {
        assertThatThrownBy(() -> new Reservation(null, theme(), time(10), LocalDate.of(2030, 5, 1)))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Reservation(member(), null, time(10), LocalDate.of(2030, 5, 1)))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Reservation(member(), theme(), null, LocalDate.of(2030, 5, 1)))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Reservation(member(), theme(), time(10), null))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("예약 소유자가 아니면 false를 반환한다")
    void differentMemberIsNotReservationOwner() {
        Reservation reservation = new Reservation(member(), theme(), time(10), LocalDate.of(2030, 5, 1));

        assertThat(reservation.isOwnedBy(new Member("코니", "cony@example.com", "password"))).isFalse();
    }

    private Member member() {
        return new Member("브라운", "brown@example.com", "password");
    }

    private Theme theme() {
        return new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
    }

    private ReservationTime time(int hour) {
        return new ReservationTime(LocalTime.of(hour, 0));
    }
}

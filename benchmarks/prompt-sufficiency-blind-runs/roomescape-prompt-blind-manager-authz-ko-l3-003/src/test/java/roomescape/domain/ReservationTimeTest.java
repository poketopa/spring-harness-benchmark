package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class ReservationTimeTest {

    @Test
    @DisplayName("예약 시간을 생성한다")
    void createReservationTime() {
        ReservationTime reservationTime = new ReservationTime(LocalTime.of(10, 0));

        assertThat(reservationTime.getStartAt()).isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    @DisplayName("예약 시간은 비어 있을 수 없다")
    void nullStartAtIsRejected() {
        assertThatThrownBy(() -> new ReservationTime(null))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}

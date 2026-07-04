package roomescape.dto;

import java.time.LocalTime;
import roomescape.domain.ReservationTime;

public record ReservationTimeResponse(Long id, LocalTime startAt, boolean reserved) {

    public static ReservationTimeResponse of(ReservationTime time, boolean reserved) {
        return new ReservationTimeResponse(time.getId(), time.getStartAt(), reserved);
    }
}

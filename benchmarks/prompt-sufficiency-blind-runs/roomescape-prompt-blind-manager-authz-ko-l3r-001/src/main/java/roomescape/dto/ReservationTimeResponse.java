package roomescape.dto;

import java.time.LocalTime;
import roomescape.domain.ReservationTime;

public record ReservationTimeResponse(Long id, LocalTime startAt, boolean reserved, Long storeId) {

    public static ReservationTimeResponse of(ReservationTime time, boolean reserved) {
        Long storeId = time.getStore() == null ? null : time.getStore().getId();
        return new ReservationTimeResponse(time.getId(), time.getStartAt(), reserved, storeId);
    }
}

package roomescape.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import roomescape.domain.Reservation;

public record ReservationResponse(
        Long id,
        LocalDate date,
        Long timeId,
        LocalTime startAt,
        Long themeId,
        String themeName,
        Long storeId,
        Long memberId,
        String memberName
) {

    public static ReservationResponse from(Reservation reservation) {
        Long storeId = reservation.getTheme().getStore() == null ? null : reservation.getTheme().getStore().getId();
        return new ReservationResponse(
                reservation.getId(),
                reservation.getDate(),
                reservation.getTime().getId(),
                reservation.getTime().getStartAt(),
                reservation.getTheme().getId(),
                reservation.getTheme().getName(),
                storeId,
                reservation.getMember().getId(),
                reservation.getMember().getName()
        );
    }
}

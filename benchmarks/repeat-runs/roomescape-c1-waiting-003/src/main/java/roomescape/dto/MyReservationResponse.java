package roomescape.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import roomescape.domain.Reservation;
import roomescape.domain.Waiting;

public record MyReservationResponse(
        Long id,
        LocalDate date,
        Long timeId,
        LocalTime startAt,
        Long themeId,
        String themeName,
        String status,
        Long rank
) {

    public static MyReservationResponse fromReservation(Reservation reservation) {
        return new MyReservationResponse(
                reservation.getId(),
                reservation.getDate(),
                reservation.getTime().getId(),
                reservation.getTime().getStartAt(),
                reservation.getTheme().getId(),
                reservation.getTheme().getName(),
                "RESERVED",
                null
        );
    }

    public static MyReservationResponse ofWaiting(Waiting waiting, long rank) {
        return new MyReservationResponse(
                waiting.getId(),
                waiting.getDate(),
                waiting.getTime().getId(),
                waiting.getTime().getStartAt(),
                waiting.getTheme().getId(),
                waiting.getTheme().getName(),
                "WAITING",
                rank
        );
    }
}

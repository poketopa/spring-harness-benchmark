package roomescape.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import roomescape.domain.Reservation;
import roomescape.domain.Waiting;

public record MyReservationResponse(
        Long id,
        Long themeId,
        String themeName,
        Long timeId,
        LocalTime startAt,
        LocalDate date,
        String status,
        Long waitingId,
        Long rank
) {

    public static MyReservationResponse fromReservation(Reservation reservation) {
        return new MyReservationResponse(
                reservation.getId(),
                reservation.getTheme().getId(),
                reservation.getTheme().getName(),
                reservation.getTime().getId(),
                reservation.getTime().getStartAt(),
                reservation.getDate(),
                "RESERVED",
                null,
                null
        );
    }

    public static MyReservationResponse ofWaiting(Waiting waiting, long rank) {
        return new MyReservationResponse(
                waiting.getId(),
                waiting.getTheme().getId(),
                waiting.getTheme().getName(),
                waiting.getTime().getId(),
                waiting.getTime().getStartAt(),
                waiting.getDate(),
                "WAITING",
                waiting.getId(),
                rank
        );
    }
}

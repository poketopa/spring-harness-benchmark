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
        Long memberId,
        String memberName,
        String status,
        Integer rank
) {

    public static MyReservationResponse reserved(Reservation reservation) {
        return new MyReservationResponse(
                reservation.getId(),
                reservation.getDate(),
                reservation.getTime().getId(),
                reservation.getTime().getStartAt(),
                reservation.getTheme().getId(),
                reservation.getTheme().getName(),
                reservation.getMember().getId(),
                reservation.getMember().getName(),
                "RESERVED",
                null
        );
    }

    public static MyReservationResponse waiting(Waiting waiting, int rank) {
        Reservation reservation = waiting.getReservation();
        return new MyReservationResponse(
                waiting.getId(),
                reservation.getDate(),
                reservation.getTime().getId(),
                reservation.getTime().getStartAt(),
                reservation.getTheme().getId(),
                reservation.getTheme().getName(),
                waiting.getMember().getId(),
                waiting.getMember().getName(),
                "WAITING",
                rank
        );
    }
}

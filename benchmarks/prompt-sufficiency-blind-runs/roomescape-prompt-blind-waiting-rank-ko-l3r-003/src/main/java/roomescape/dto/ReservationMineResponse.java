package roomescape.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import roomescape.domain.Reservation;
import roomescape.domain.Waiting;

public record ReservationMineResponse(
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

    private static final String RESERVED = "RESERVED";
    private static final String WAITING = "WAITING";

    public static ReservationMineResponse from(Reservation reservation) {
        return new ReservationMineResponse(
                reservation.getId(),
                reservation.getDate(),
                reservation.getTime().getId(),
                reservation.getTime().getStartAt(),
                reservation.getTheme().getId(),
                reservation.getTheme().getName(),
                reservation.getMember().getId(),
                reservation.getMember().getName(),
                RESERVED,
                null
        );
    }

    public static ReservationMineResponse of(Waiting waiting, int rank) {
        return new ReservationMineResponse(
                waiting.getId(),
                waiting.getDate(),
                waiting.getTime().getId(),
                waiting.getTime().getStartAt(),
                waiting.getTheme().getId(),
                waiting.getTheme().getName(),
                waiting.getMember().getId(),
                waiting.getMember().getName(),
                WAITING,
                rank
        );
    }
}

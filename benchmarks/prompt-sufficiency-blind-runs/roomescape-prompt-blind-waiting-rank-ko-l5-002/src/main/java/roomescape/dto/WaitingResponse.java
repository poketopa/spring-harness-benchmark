package roomescape.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import roomescape.domain.Reservation;
import roomescape.domain.Waiting;

public record WaitingResponse(
        Long id,
        LocalDate date,
        Long timeId,
        LocalTime startAt,
        Long themeId,
        String themeName,
        Long memberId,
        String memberName,
        int rank
) {

    public static WaitingResponse of(Waiting waiting, int rank) {
        Reservation reservation = waiting.getReservation();
        return new WaitingResponse(
                waiting.getId(),
                reservation.getDate(),
                reservation.getTime().getId(),
                reservation.getTime().getStartAt(),
                reservation.getTheme().getId(),
                reservation.getTheme().getName(),
                waiting.getMember().getId(),
                waiting.getMember().getName(),
                rank
        );
    }
}

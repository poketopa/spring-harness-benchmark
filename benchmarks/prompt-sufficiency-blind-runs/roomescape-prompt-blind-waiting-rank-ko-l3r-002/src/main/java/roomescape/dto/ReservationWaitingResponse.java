package roomescape.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import roomescape.domain.ReservationWaiting;

public record ReservationWaitingResponse(
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

    public static ReservationWaitingResponse of(ReservationWaiting waiting, int rank) {
        return new ReservationWaitingResponse(
                waiting.getId(),
                waiting.getDate(),
                waiting.getTime().getId(),
                waiting.getTime().getStartAt(),
                waiting.getTheme().getId(),
                waiting.getTheme().getName(),
                waiting.getMember().getId(),
                waiting.getMember().getName(),
                rank
        );
    }
}

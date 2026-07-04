package roomescape.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import roomescape.domain.Waiting;

public record WaitingResponse(
        Long id,
        Long memberId,
        Long themeId,
        String themeName,
        Long timeId,
        LocalTime startAt,
        LocalDate date,
        long rank
) {

    public static WaitingResponse of(Waiting waiting, long rank) {
        return new WaitingResponse(
                waiting.getId(),
                waiting.getMember().getId(),
                waiting.getTheme().getId(),
                waiting.getTheme().getName(),
                waiting.getTime().getId(),
                waiting.getTime().getStartAt(),
                waiting.getDate(),
                rank
        );
    }
}

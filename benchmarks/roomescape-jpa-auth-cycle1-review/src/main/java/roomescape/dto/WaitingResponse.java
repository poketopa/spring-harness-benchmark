package roomescape.dto;

import java.time.LocalDate;
import java.time.LocalTime;
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
        long waitingRank
) {

    public static WaitingResponse from(Waiting waiting, long waitingRank) {
        return new WaitingResponse(
                waiting.getId(),
                waiting.getDate(),
                waiting.getTime().getId(),
                waiting.getTime().getStartAt(),
                waiting.getTheme().getId(),
                waiting.getTheme().getName(),
                waiting.getMember().getId(),
                waiting.getMember().getName(),
                waitingRank
        );
    }
}

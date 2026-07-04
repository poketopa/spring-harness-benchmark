package roomescape.dto;

import roomescape.domain.Theme;

public record ThemeResponse(Long id, String name, String description, String thumbnailUrl, Long managerId) {

    public static ThemeResponse from(Theme theme) {
        return new ThemeResponse(
                theme.getId(),
                theme.getName(),
                theme.getDescription(),
                theme.getThumbnailUrl(),
                theme.getManager() == null ? null : theme.getManager().getId()
        );
    }
}

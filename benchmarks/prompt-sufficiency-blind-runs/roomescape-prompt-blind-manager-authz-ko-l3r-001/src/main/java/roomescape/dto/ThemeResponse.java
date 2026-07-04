package roomescape.dto;

import roomescape.domain.Theme;

public record ThemeResponse(Long id, String name, String description, String thumbnailUrl, Long storeId) {

    public static ThemeResponse from(Theme theme) {
        Long storeId = theme.getStore() == null ? null : theme.getStore().getId();
        return new ThemeResponse(
                theme.getId(),
                theme.getName(),
                theme.getDescription(),
                theme.getThumbnailUrl(),
                storeId
        );
    }
}

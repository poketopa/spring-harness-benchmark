package roomescape.dto;

import roomescape.domain.Theme;

public record ThemeResponse(Long id, String name, String description, String thumbnailUrl, Long storeId, String storeName) {

    public static ThemeResponse from(Theme theme) {
        Long storeId = null;
        String storeName = null;
        if (theme.getStore() != null) {
            storeId = theme.getStore().getId();
            storeName = theme.getStore().getName();
        }
        return new ThemeResponse(
                theme.getId(),
                theme.getName(),
                theme.getDescription(),
                theme.getThumbnailUrl(),
                storeId,
                storeName
        );
    }
}

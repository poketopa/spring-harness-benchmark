package roomescape.dto;

import roomescape.domain.Store;

public record StoreResponse(
        Long id,
        String name,
        Long managerId,
        String managerName
) {

    public static StoreResponse from(Store store) {
        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getManager().getId(),
                store.getManager().getName()
        );
    }
}

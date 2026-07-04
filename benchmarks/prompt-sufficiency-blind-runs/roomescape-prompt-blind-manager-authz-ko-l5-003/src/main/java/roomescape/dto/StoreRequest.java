package roomescape.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StoreRequest(
        @NotBlank String name,
        @NotNull Long managerId
) {
}

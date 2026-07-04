package roomescape.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StoreRequest(
        @NotBlank String name,
        @NotNull @Positive Long managerId
) {
}

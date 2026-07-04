package roomescape.dto;

import jakarta.validation.constraints.NotBlank;

public record StoreRequest(@NotBlank String name) {
}

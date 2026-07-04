package roomescape.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ManagerRequest(
        @NotBlank String name,
        @Email @NotBlank String email,
        @NotBlank String password
) {
}

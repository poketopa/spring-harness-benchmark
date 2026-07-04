package roomescape.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ReservationChangeRequest(
        @NotNull LocalDate date,
        @NotNull @Positive Long timeId
) {
}

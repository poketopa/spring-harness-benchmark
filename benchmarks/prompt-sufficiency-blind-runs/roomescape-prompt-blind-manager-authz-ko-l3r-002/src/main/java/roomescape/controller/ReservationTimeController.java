package roomescape.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import roomescape.dto.ReservationTimeRequest;
import roomescape.dto.ReservationTimeResponse;
import roomescape.service.ReservationTimeService;

@RestController
public class ReservationTimeController {

    private final ReservationTimeService reservationTimeService;

    public ReservationTimeController(ReservationTimeService reservationTimeService) {
        this.reservationTimeService = reservationTimeService;
    }

    @PostMapping("/admin/times")
    public ResponseEntity<ReservationTimeResponse> create(@Valid @RequestBody ReservationTimeRequest request) {
        ReservationTimeResponse response = reservationTimeService.create(request);
        return created(response);
    }

    @PostMapping("/admin/stores/{storeId}/times")
    public ResponseEntity<ReservationTimeResponse> create(
            @PathVariable Long storeId,
            @Valid @RequestBody ReservationTimeRequest request
    ) {
        ReservationTimeResponse response = reservationTimeService.create(storeId, request);
        return created(response);
    }

    private ResponseEntity<ReservationTimeResponse> created(ReservationTimeResponse response) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/themes/{themeId}/times")
    public List<ReservationTimeResponse> findThemeTimes(
            @PathVariable Long themeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return reservationTimeService.findThemeTimes(themeId, date);
    }

    @DeleteMapping("/admin/times/{timeId}")
    public ResponseEntity<Void> delete(@PathVariable Long timeId) {
        reservationTimeService.delete(timeId);
        return ResponseEntity.noContent().build();
    }
}

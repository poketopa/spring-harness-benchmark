package roomescape.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import roomescape.auth.LoginMember;
import roomescape.dto.MyReservationResponse;
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
import roomescape.dto.WaitingResponse;
import roomescape.service.ReservationService;
import roomescape.service.WaitingService;

@RestController
public class ReservationController {

    private final ReservationService reservationService;
    private final WaitingService waitingService;

    public ReservationController(ReservationService reservationService, WaitingService waitingService) {
        this.reservationService = reservationService;
        this.waitingService = waitingService;
    }

    @PostMapping("/reservations")
    public ResponseEntity<ReservationResponse> create(
            LoginMember loginMember,
            @Valid @RequestBody ReservationRequest request
    ) {
        ReservationResponse response = reservationService.create(loginMember, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/reservations/waitings")
    public ResponseEntity<WaitingResponse> createWaiting(
            LoginMember loginMember,
            @Valid @RequestBody ReservationRequest request
    ) {
        WaitingResponse response = waitingService.create(loginMember, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/reservations/mine")
    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        return reservationService.findMine(loginMember);
    }
}

package roomescape.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import roomescape.auth.LoginMember;
import roomescape.dto.MyReservationResponse;
import roomescape.dto.ReservationChangeRequest;
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
import roomescape.service.MyReservationService;
import roomescape.service.ReservationService;

@RestController
public class ReservationController {

    private final ReservationService reservationService;
    private final MyReservationService myReservationService;

    public ReservationController(ReservationService reservationService, MyReservationService myReservationService) {
        this.reservationService = reservationService;
        this.myReservationService = myReservationService;
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

    @GetMapping("/reservations/mine")
    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        return myReservationService.findMine(loginMember);
    }

    @GetMapping("/manager/reservations")
    public List<ReservationResponse> findManaged(LoginMember loginMember) {
        return reservationService.findManaged(loginMember);
    }

    @GetMapping("/manager/reservations/{reservationId}")
    public ReservationResponse findManaged(LoginMember loginMember, @PathVariable Long reservationId) {
        return reservationService.findManaged(loginMember, reservationId);
    }

    @PutMapping("/reservations/{reservationId}")
    public ReservationResponse change(
            LoginMember loginMember,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationChangeRequest request
    ) {
        return reservationService.change(loginMember, reservationId, request);
    }

    @PutMapping("/manager/reservations/{reservationId}")
    public ReservationResponse changeManaged(
            LoginMember loginMember,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationChangeRequest request
    ) {
        return reservationService.changeManaged(loginMember, reservationId, request);
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> cancel(LoginMember loginMember, @PathVariable Long reservationId) {
        reservationService.cancel(loginMember, reservationId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/manager/reservations/{reservationId}")
    public ResponseEntity<Void> cancelManaged(LoginMember loginMember, @PathVariable Long reservationId) {
        reservationService.cancelManaged(loginMember, reservationId);
        return ResponseEntity.noContent().build();
    }
}

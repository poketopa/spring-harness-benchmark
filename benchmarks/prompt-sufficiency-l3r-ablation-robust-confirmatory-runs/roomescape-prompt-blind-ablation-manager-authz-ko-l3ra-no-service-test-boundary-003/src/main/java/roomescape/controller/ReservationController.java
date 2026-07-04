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
import roomescape.service.ManagerReservationService;
import roomescape.service.MyReservationService;
import roomescape.service.ReservationService;

@RestController
public class ReservationController {

    private final ReservationService reservationService;
    private final MyReservationService myReservationService;
    private final ManagerReservationService managerReservationService;

    public ReservationController(
            ReservationService reservationService,
            MyReservationService myReservationService,
            ManagerReservationService managerReservationService
    ) {
        this.reservationService = reservationService;
        this.myReservationService = myReservationService;
        this.managerReservationService = managerReservationService;
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
    public List<ReservationResponse> findManagedReservations(LoginMember loginMember) {
        return managerReservationService.findManagedReservations(loginMember);
    }

    @PutMapping("/reservations/{reservationId}")
    public ReservationResponse change(
            LoginMember loginMember,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationChangeRequest request
    ) {
        return reservationService.change(loginMember, reservationId, request);
    }

    @DeleteMapping("/reservations/{reservationId}")
    public ResponseEntity<Void> cancel(LoginMember loginMember, @PathVariable Long reservationId) {
        reservationService.cancel(loginMember, reservationId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/manager/reservations/{reservationId}")
    public ReservationResponse changeManagedReservation(
            LoginMember loginMember,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationChangeRequest request
    ) {
        return managerReservationService.change(loginMember, reservationId, request);
    }

    @DeleteMapping("/manager/reservations/{reservationId}")
    public ResponseEntity<Void> cancelManagedReservation(LoginMember loginMember, @PathVariable Long reservationId) {
        managerReservationService.cancel(loginMember, reservationId);
        return ResponseEntity.noContent().build();
    }
}

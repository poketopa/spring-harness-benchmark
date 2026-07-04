package roomescape.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.LoginMember;
import roomescape.dto.ReservationChangeRequest;
import roomescape.dto.ReservationResponse;
import roomescape.service.ReservationService;

@RestController
public class ManagerReservationController {

    private final ReservationService reservationService;

    public ManagerReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/manager/reservations")
    public List<ReservationResponse> findAll(LoginMember loginMember) {
        return reservationService.findAllForManager(loginMember);
    }

    @GetMapping("/manager/reservations/{reservationId}")
    public ReservationResponse find(LoginMember loginMember, @PathVariable Long reservationId) {
        return reservationService.findForManager(loginMember, reservationId);
    }

    @PutMapping("/manager/reservations/{reservationId}")
    public ReservationResponse change(
            LoginMember loginMember,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationChangeRequest request
    ) {
        return reservationService.changeForManager(loginMember, reservationId, request);
    }

    @DeleteMapping("/manager/reservations/{reservationId}")
    public ResponseEntity<Void> cancel(LoginMember loginMember, @PathVariable Long reservationId) {
        reservationService.cancelForManager(loginMember, reservationId);
        return ResponseEntity.noContent().build();
    }
}

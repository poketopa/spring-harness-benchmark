package roomescape.controller;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import roomescape.auth.LoginMember;
import roomescape.dto.ReservationChangeRequest;
import roomescape.dto.ReservationResponse;
import roomescape.service.ManagerReservationService;

@RestController
public class ManagerReservationController {

    private final ManagerReservationService managerReservationService;

    public ManagerReservationController(ManagerReservationService managerReservationService) {
        this.managerReservationService = managerReservationService;
    }

    @GetMapping("/admin/reservations")
    public List<ReservationResponse> findAll(LoginMember loginMember, @RequestParam Long storeId) {
        return managerReservationService.findAll(loginMember, storeId);
    }

    @PutMapping("/admin/reservations/{reservationId}")
    public ReservationResponse change(
            LoginMember loginMember,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationChangeRequest request
    ) {
        return managerReservationService.change(loginMember, reservationId, request);
    }

    @DeleteMapping("/admin/reservations/{reservationId}")
    public ResponseEntity<Void> cancel(LoginMember loginMember, @PathVariable Long reservationId) {
        managerReservationService.cancel(loginMember, reservationId);
        return ResponseEntity.noContent().build();
    }
}

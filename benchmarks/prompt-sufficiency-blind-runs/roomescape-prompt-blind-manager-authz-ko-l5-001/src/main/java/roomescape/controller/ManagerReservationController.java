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
import roomescape.service.ManagerReservationService;

@RestController
public class ManagerReservationController {

    private final ManagerReservationService managerReservationService;

    public ManagerReservationController(ManagerReservationService managerReservationService) {
        this.managerReservationService = managerReservationService;
    }

    @GetMapping("/manager/stores/{storeId}/reservations")
    public List<ReservationResponse> findByStore(LoginMember loginMember, @PathVariable Long storeId) {
        return managerReservationService.findByStore(loginMember, storeId);
    }

    @PutMapping("/manager/stores/{storeId}/reservations/{reservationId}")
    public ReservationResponse change(
            LoginMember loginMember,
            @PathVariable Long storeId,
            @PathVariable Long reservationId,
            @Valid @RequestBody ReservationChangeRequest request
    ) {
        return managerReservationService.change(loginMember, storeId, reservationId, request);
    }

    @DeleteMapping("/manager/stores/{storeId}/reservations/{reservationId}")
    public ResponseEntity<Void> cancel(
            LoginMember loginMember,
            @PathVariable Long storeId,
            @PathVariable Long reservationId
    ) {
        managerReservationService.cancel(loginMember, storeId, reservationId);
        return ResponseEntity.noContent().build();
    }
}

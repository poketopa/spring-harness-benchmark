package roomescape.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import roomescape.auth.LoginMember;
import roomescape.dto.WaitingRequest;
import roomescape.dto.WaitingResponse;
import roomescape.service.WaitingService;

@RestController
public class WaitingController {

    private final WaitingService waitingService;

    public WaitingController(WaitingService waitingService) {
        this.waitingService = waitingService;
    }

    @PostMapping("/waitings")
    public ResponseEntity<WaitingResponse> create(
            LoginMember loginMember,
            @Valid @RequestBody WaitingRequest request
    ) {
        WaitingResponse response = waitingService.create(loginMember, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/admin/waitings")
    public List<WaitingResponse> findAll(LoginMember loginMember) {
        return waitingService.findAll(loginMember);
    }

    @DeleteMapping("/waitings/{waitingId}")
    public ResponseEntity<Void> cancel(LoginMember loginMember, @PathVariable Long waitingId) {
        waitingService.cancel(loginMember, waitingId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/waitings/{waitingId}")
    public ResponseEntity<Void> cancelByAdmin(LoginMember loginMember, @PathVariable Long waitingId) {
        waitingService.cancelByAdmin(loginMember, waitingId);
        return ResponseEntity.noContent().build();
    }
}

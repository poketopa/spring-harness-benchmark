package roomescape.controller;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @DeleteMapping("/waitings/{id}")
    public ResponseEntity<Void> delete(LoginMember loginMember, @PathVariable Long id) {
        waitingService.delete(loginMember, id);
        return ResponseEntity.noContent().build();
    }
}

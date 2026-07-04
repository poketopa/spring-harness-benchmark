package roomescape.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import roomescape.dto.ThemeRequest;
import roomescape.dto.ThemeResponse;
import roomescape.service.ThemeService;

@RestController
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @PostMapping("/admin/themes")
    public ResponseEntity<ThemeResponse> create(@Valid @RequestBody ThemeRequest request) {
        ThemeResponse response = themeService.create(request);
        return created(response);
    }

    @PostMapping("/admin/stores/{storeId}/themes")
    public ResponseEntity<ThemeResponse> create(
            @PathVariable Long storeId,
            @Valid @RequestBody ThemeRequest request
    ) {
        ThemeResponse response = themeService.create(storeId, request);
        return created(response);
    }

    private ResponseEntity<ThemeResponse> created(ThemeResponse response) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/themes")
    public List<ThemeResponse> findAll() {
        return themeService.findAll();
    }
}

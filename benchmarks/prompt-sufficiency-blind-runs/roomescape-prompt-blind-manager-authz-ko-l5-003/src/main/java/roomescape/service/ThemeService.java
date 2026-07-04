package roomescape.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Theme;
import roomescape.dto.ThemeRequest;
import roomescape.dto.ThemeResponse;
import roomescape.repository.ThemeRepository;
import roomescape.repository.StoreRepository;

@Service
@Transactional(readOnly = true)
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final StoreRepository storeRepository;

    public ThemeService(ThemeRepository themeRepository, StoreRepository storeRepository) {
        this.themeRepository = themeRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional
    public ThemeResponse create(ThemeRequest request) {
        Theme theme = createTheme(request);
        return ThemeResponse.from(themeRepository.save(theme));
    }

    private Theme createTheme(ThemeRequest request) {
        if (request.storeId() == null) {
            return new Theme(request.name(), request.description(), request.thumbnailUrl());
        }
        return new Theme(
                request.name(),
                request.description(),
                request.thumbnailUrl(),
                storeRepository.getByIdOrThrow(request.storeId())
        );
    }

    public List<ThemeResponse> findAll() {
        return themeRepository.findAll()
                .stream()
                .map(ThemeResponse::from)
                .toList();
    }
}

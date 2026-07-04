package roomescape.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Theme;
import roomescape.dto.ThemeRequest;
import roomescape.dto.ThemeResponse;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

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
        Theme theme = new Theme(
                request.name(),
                request.description(),
                request.thumbnailUrl(),
                request.storeId() == null ? null : storeRepository.getByIdOrThrow(request.storeId())
        );
        return ThemeResponse.from(themeRepository.save(theme));
    }

    public List<ThemeResponse> findAll() {
        return themeRepository.findAll()
                .stream()
                .map(ThemeResponse::from)
                .toList();
    }
}

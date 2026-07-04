package roomescape.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.dto.ThemeRequest;
import roomescape.dto.ThemeResponse;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

@Service
@Transactional(readOnly = true)
public class ThemeService {

    private static final String DEFAULT_STORE_NAME = "기본 매장";

    private final ThemeRepository themeRepository;
    private final StoreRepository storeRepository;

    public ThemeService(ThemeRepository themeRepository, StoreRepository storeRepository) {
        this.themeRepository = themeRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional
    public ThemeResponse create(ThemeRequest request) {
        Store store = findStore(request.storeId());
        Theme theme = new Theme(request.name(), request.description(), request.thumbnailUrl(), store);
        return ThemeResponse.from(themeRepository.save(theme));
    }

    private Store findStore(Long storeId) {
        if (storeId != null) {
            return storeRepository.getByIdOrThrow(storeId);
        }
        return storeRepository.findByName(DEFAULT_STORE_NAME)
                .orElseGet(() -> storeRepository.save(new Store(DEFAULT_STORE_NAME)));
    }

    public List<ThemeResponse> findAll() {
        return themeRepository.findAll()
                .stream()
                .map(ThemeResponse::from)
                .toList();
    }
}

package roomescape.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.dto.ThemeRequest;
import roomescape.dto.ThemeResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

@Service
@Transactional(readOnly = true)
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final StoreRepository storeRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public ThemeService(
            ThemeRepository themeRepository,
            StoreRepository storeRepository,
            AdminAuthorizationService adminAuthorizationService
    ) {
        this.themeRepository = themeRepository;
        this.storeRepository = storeRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional
    public ThemeResponse create(LoginMember loginMember, ThemeRequest request) {
        adminAuthorizationService.validateAdmin(loginMember);
        Store store = findStore(request.storeId());
        Theme theme = new Theme(request.name(), request.description(), request.thumbnailUrl(), store);
        return ThemeResponse.from(themeRepository.save(theme));
    }

    private Store findStore(Long storeId) {
        if (storeId == null) {
            return null;
        }
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.STORE_NOT_FOUND, "매장을 찾을 수 없습니다."));
    }

    public List<ThemeResponse> findAll() {
        return themeRepository.findAll()
                .stream()
                .map(ThemeResponse::from)
                .toList();
    }
}

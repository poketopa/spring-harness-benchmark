package roomescape.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.domain.Theme;
import roomescape.dto.ThemeRequest;
import roomescape.dto.ThemeResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ThemeRepository;

@Service
@Transactional(readOnly = true)
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final MemberRepository memberRepository;

    public ThemeService(ThemeRepository themeRepository, MemberRepository memberRepository) {
        this.themeRepository = themeRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public ThemeResponse create(ThemeRequest request) {
        Member manager = findManager(request.managerId());
        Theme theme = new Theme(request.name(), request.description(), request.thumbnailUrl(), manager);
        return ThemeResponse.from(themeRepository.save(theme));
    }

    private Member findManager(Long managerId) {
        if (managerId == null) {
            return null;
        }
        Member manager = memberRepository.findById(managerId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.INVALID_INPUT, "매니저를 찾을 수 없습니다."));
        if (manager.getRole() != Role.MANAGER) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매니저 권한이 있는 회원만 매장을 담당할 수 있습니다.");
        }
        return manager;
    }

    public List<ThemeResponse> findAll() {
        return themeRepository.findAll()
                .stream()
                .map(ThemeResponse::from)
                .toList();
    }
}

package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Store;
import roomescape.dto.StoreRequest;
import roomescape.dto.StoreResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.StoreRepository;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final MemberRepository memberRepository;
    private final AdminAuthorizationService adminAuthorizationService;

    public StoreService(
            StoreRepository storeRepository,
            MemberRepository memberRepository,
            AdminAuthorizationService adminAuthorizationService
    ) {
        this.storeRepository = storeRepository;
        this.memberRepository = memberRepository;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Transactional
    public StoreResponse create(LoginMember loginMember, StoreRequest request) {
        adminAuthorizationService.validateAdmin(loginMember);
        Member manager = findManager(request.managerId());
        if (storeRepository.existsByManager(manager)) {
            throw new RoomescapeException(ErrorCode.MANAGER_ALREADY_HAS_STORE, "이미 매장을 관리 중인 매니저입니다.");
        }

        Store store = new Store(request.name(), manager);
        return StoreResponse.from(storeRepository.save(store));
    }

    private Member findManager(Long managerId) {
        Member member = memberRepository.findById(managerId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다."));
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매니저만 매장을 관리할 수 있습니다.");
        }
        return member;
    }
}

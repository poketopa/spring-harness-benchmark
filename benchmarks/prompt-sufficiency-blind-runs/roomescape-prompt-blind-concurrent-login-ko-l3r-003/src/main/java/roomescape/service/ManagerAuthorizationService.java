package roomescape.service;

import org.springframework.stereotype.Service;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Store;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.StoreRepository;

@Service
public class ManagerAuthorizationService {

    private final AuthenticatedMemberService authenticatedMemberService;
    private final StoreRepository storeRepository;

    public ManagerAuthorizationService(
            AuthenticatedMemberService authenticatedMemberService,
            StoreRepository storeRepository
    ) {
        this.authenticatedMemberService = authenticatedMemberService;
        this.storeRepository = storeRepository;
    }

    public Store findManagedStore(LoginMember loginMember) {
        Member manager = authenticatedMemberService.findMember(loginMember);
        validateManagerRole(manager);
        return storeRepository.findByManager(manager)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.FORBIDDEN, "관리 중인 매장이 없습니다."));
    }

    private void validateManagerRole(Member member) {
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매니저 권한이 필요합니다.");
        }
    }
}

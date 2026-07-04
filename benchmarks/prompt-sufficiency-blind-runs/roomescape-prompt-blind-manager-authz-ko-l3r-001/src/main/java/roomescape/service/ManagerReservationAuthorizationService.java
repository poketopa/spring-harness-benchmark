package roomescape.service;

import org.springframework.stereotype.Service;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Store;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ManagerStoreRepository;

@Service
public class ManagerReservationAuthorizationService {

    private final AuthenticatedMemberService authenticatedMemberService;
    private final ManagerStoreRepository managerStoreRepository;

    public ManagerReservationAuthorizationService(
            AuthenticatedMemberService authenticatedMemberService,
            ManagerStoreRepository managerStoreRepository
    ) {
        this.authenticatedMemberService = authenticatedMemberService;
        this.managerStoreRepository = managerStoreRepository;
    }

    public Member authorizeStoreAccess(LoginMember loginMember, Store store) {
        Member manager = authenticatedMemberService.findMember(loginMember);
        validateManager(manager);
        validateManagerStore(manager, store);
        return manager;
    }

    private void validateManager(Member member) {
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매니저 권한이 필요합니다.");
        }
    }

    private void validateManagerStore(Member manager, Store store) {
        if (!managerStoreRepository.existsByManagerAndStore(manager, store)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리 중인 매장의 예약만 접근할 수 있습니다.");
        }
    }
}

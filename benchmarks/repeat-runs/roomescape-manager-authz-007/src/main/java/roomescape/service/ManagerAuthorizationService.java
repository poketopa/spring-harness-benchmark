package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.StoreRepository;

@Service
@Transactional(readOnly = true)
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
        validateManager(manager);
        return storeRepository.findByManager(manager)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.FORBIDDEN, "관리하는 매장이 없습니다."));
    }

    private void validateManager(Member manager) {
        if (manager.getRole() != Role.MANAGER) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매니저 권한이 필요합니다.");
        }
    }

    public void validateCanManage(LoginMember loginMember, Reservation reservation) {
        Store store = findManagedStore(loginMember);
        if (!reservation.isInStore(store)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리하는 매장의 예약만 접근할 수 있습니다.");
        }
    }
}

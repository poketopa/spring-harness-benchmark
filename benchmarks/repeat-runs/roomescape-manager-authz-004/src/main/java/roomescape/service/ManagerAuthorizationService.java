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

    public Store authorizeStore(LoginMember loginMember) {
        Member manager = authenticatedMemberService.findMember(loginMember);
        if (manager.getRole() != Role.MANAGER) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매니저 권한이 필요합니다.");
        }
        return storeRepository.findByManager(manager)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.FORBIDDEN, "관리 중인 매장이 없습니다."));
    }

    public void validateManagedReservation(Store store, Reservation reservation) {
        if (!reservation.isManagedBy(store)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "담당 매장의 예약만 관리할 수 있습니다.");
        }
    }
}

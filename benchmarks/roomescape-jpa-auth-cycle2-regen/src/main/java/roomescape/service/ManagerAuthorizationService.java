package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
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
        Member manager = validateManager(loginMember);
        return storeRepository.findByManager(manager)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.FORBIDDEN, "관리하는 매장이 없습니다."));
    }

    private Member validateManager(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매니저 권한이 필요합니다.");
        }
        return member;
    }

    public void validateManagedReservation(LoginMember loginMember, Reservation reservation) {
        Member manager = validateManager(loginMember);
        if (!reservation.isManagedBy(manager)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "해당 매장의 예약만 관리할 수 있습니다.");
        }
    }
}

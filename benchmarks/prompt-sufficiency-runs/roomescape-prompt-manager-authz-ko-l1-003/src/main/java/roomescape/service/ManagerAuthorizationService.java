package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Service
@Transactional(readOnly = true)
public class ManagerAuthorizationService {

    private final AuthenticatedMemberService authenticatedMemberService;

    public ManagerAuthorizationService(AuthenticatedMemberService authenticatedMemberService) {
        this.authenticatedMemberService = authenticatedMemberService;
    }

    public Member authorizeManager(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);
        if (!member.isManager()) {
            throw forbidden("매니저 권한이 필요합니다.");
        }
        return member;
    }

    public void authorizeStoreManager(Member manager, Reservation reservation) {
        if (!reservation.getTheme().isManagedBy(manager)) {
            throw forbidden("예약 매장에 대한 권한이 없습니다.");
        }
    }

    private RoomescapeException forbidden(String message) {
        return new RoomescapeException(ErrorCode.FORBIDDEN, message);
    }
}

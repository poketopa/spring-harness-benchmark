package roomescape.service;

import org.springframework.stereotype.Service;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Service
public class ManagerAuthorizationService {

    private final AuthenticatedMemberService authenticatedMemberService;

    public ManagerAuthorizationService(AuthenticatedMemberService authenticatedMemberService) {
        this.authenticatedMemberService = authenticatedMemberService;
    }

    public Member findManager(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "매니저 권한이 필요합니다.");
        }
        return member;
    }
}

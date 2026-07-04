package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Service
@Transactional(readOnly = true)
public class AdminAuthorizationService {

    private final AuthenticatedMemberService authenticatedMemberService;

    public AdminAuthorizationService(AuthenticatedMemberService authenticatedMemberService) {
        this.authenticatedMemberService = authenticatedMemberService;
    }

    public void validateAdmin(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);
        if (!member.isAdmin()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "어드민 권한이 필요합니다.");
        }
    }
}

package roomescape.service;

import org.springframework.stereotype.Service;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;

@Service
public class AuthenticatedMemberService {

    private final MemberRepository memberRepository;

    public AuthenticatedMemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public Member findMember(LoginMember loginMember) {
        return memberRepository.findById(loginMember.id())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
    }
}

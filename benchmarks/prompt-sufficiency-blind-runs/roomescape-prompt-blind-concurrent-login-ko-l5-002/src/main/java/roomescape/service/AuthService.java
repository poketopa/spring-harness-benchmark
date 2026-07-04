package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.AuthTokenPayload;
import roomescape.auth.AuthTokenProvider;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.MemberSession;
import roomescape.dto.LoginRequest;
import roomescape.dto.LoginResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.MemberSessionRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final MemberSessionRepository memberSessionRepository;
    private final AuthTokenProvider authTokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            MemberSessionRepository memberSessionRepository,
            AuthTokenProvider authTokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.memberSessionRepository = memberSessionRepository;
        this.authTokenProvider = authTokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findWithLockByEmail(request.email())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.hasPassword(request.password())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String sessionKey = authTokenProvider.createSessionKey();
        renewSession(member, sessionKey);
        return new LoginResponse(authTokenProvider.createToken(member, sessionKey));
    }

    public LoginMember authenticate(String accessToken) {
        AuthTokenPayload payload = authTokenProvider.extractPayload(accessToken);
        if (!memberSessionRepository.existsByMemberIdAndSessionKey(payload.memberId(), payload.sessionKey())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
        Member member = memberRepository.findById(payload.memberId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        return new LoginMember(member.getId(), member.getName());
    }

    private void renewSession(Member member, String sessionKey) {
        memberSessionRepository.findByMemberId(member.getId())
                .ifPresentOrElse(
                        session -> session.renew(sessionKey),
                        () -> memberSessionRepository.save(new MemberSession(member, sessionKey))
                );
    }
}

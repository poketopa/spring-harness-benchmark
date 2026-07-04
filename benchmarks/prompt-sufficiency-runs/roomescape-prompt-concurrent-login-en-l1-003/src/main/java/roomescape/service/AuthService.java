package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.AuthTokenProvider;
import roomescape.auth.AuthTokenProvider.AuthTokenPayload;
import roomescape.auth.LoginMember;
import roomescape.domain.LoginSession;
import roomescape.domain.Member;
import roomescape.dto.LoginRequest;
import roomescape.dto.LoginResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.LoginSessionRepository;
import roomescape.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final LoginSessionRepository loginSessionRepository;
    private final AuthTokenProvider authTokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            LoginSessionRepository loginSessionRepository,
            AuthTokenProvider authTokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.loginSessionRepository = loginSessionRepository;
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
        renewLoginSession(member, sessionKey);
        return new LoginResponse(authTokenProvider.createToken(member, sessionKey));
    }

    private void renewLoginSession(Member member, String sessionKey) {
        LoginSession loginSession = loginSessionRepository.findByMember(member)
                .orElseGet(() -> new LoginSession(member, sessionKey));
        loginSession.renew(sessionKey);
        loginSessionRepository.saveAndFlush(loginSession);
    }

    public LoginMember authenticate(String accessToken) {
        AuthTokenPayload payload = authTokenProvider.extractPayload(accessToken);
        Member member = memberRepository.findById(payload.memberId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        validateActiveSession(member, payload.sessionKey());
        return new LoginMember(member.getId(), member.getName());
    }

    private void validateActiveSession(Member member, String sessionKey) {
        LoginSession loginSession = loginSessionRepository.findByMember(member)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        if (!loginSession.hasSessionKey(sessionKey)) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }
}

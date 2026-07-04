package roomescape.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.AuthToken;
import roomescape.auth.AuthTokenProvider;
import roomescape.auth.LoginMember;
import roomescape.domain.AuthSession;
import roomescape.domain.Member;
import roomescape.dto.LoginRequest;
import roomescape.dto.LoginResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.AuthSessionRepository;
import roomescape.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final AuthSessionRepository authSessionRepository;
    private final AuthTokenProvider authTokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            AuthSessionRepository authSessionRepository,
            AuthTokenProvider authTokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.authSessionRepository = authSessionRepository;
        this.authTokenProvider = authTokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.hasPassword(request.password())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        AuthSession session = renewSession(member);
        return new LoginResponse(authTokenProvider.createToken(member, session.getSessionKey()));
    }

    private AuthSession renewSession(Member member) {
        String sessionKey = UUID.randomUUID().toString();
        AuthSession session = authSessionRepository.findByMember(member)
                .orElseGet(() -> new AuthSession(member, sessionKey));
        session.rotate(sessionKey);
        return authSessionRepository.saveAndFlush(session);
    }

    public LoginMember authenticate(String accessToken) {
        AuthToken token = authTokenProvider.extractToken(accessToken);
        Member member = findAuthenticatedMember(token);
        validateCurrentSession(member, token);
        return new LoginMember(member.getId(), member.getName());
    }

    private Member findAuthenticatedMember(AuthToken token) {
        return memberRepository.findById(token.memberId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
    }

    private void validateCurrentSession(Member member, AuthToken token) {
        boolean validSession = authSessionRepository.findByMember(member)
                .filter(session -> session.matches(token.sessionKey()))
                .isPresent();
        if (!validSession) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "다른 기기에서 로그인되어 인증이 만료되었습니다.");
        }
    }
}

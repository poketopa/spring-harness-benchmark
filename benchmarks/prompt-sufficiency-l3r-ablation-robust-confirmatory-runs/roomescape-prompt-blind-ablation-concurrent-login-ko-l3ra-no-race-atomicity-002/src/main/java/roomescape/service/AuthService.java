package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.AuthTokenPayload;
import roomescape.auth.AuthTokenProvider;
import roomescape.auth.LoginMember;
import roomescape.domain.ActiveSession;
import roomescape.domain.Member;
import roomescape.dto.LoginRequest;
import roomescape.dto.LoginResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ActiveSessionRepository;
import roomescape.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final ActiveSessionRepository activeSessionRepository;
    private final AuthTokenProvider authTokenProvider;

    public AuthService(
            MemberRepository memberRepository,
            ActiveSessionRepository activeSessionRepository,
            AuthTokenProvider authTokenProvider
    ) {
        this.memberRepository = memberRepository;
        this.activeSessionRepository = activeSessionRepository;
        this.authTokenProvider = authTokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmailForUpdate(request.email())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.hasPassword(request.password())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String sessionId = authTokenProvider.newSessionId();
        renewActiveSession(member, sessionId);
        return new LoginResponse(authTokenProvider.createToken(member.getId(), sessionId));
    }

    public LoginMember authenticate(String accessToken) {
        AuthTokenPayload payload = authTokenProvider.extractPayload(accessToken);
        ActiveSession activeSession = activeSessionRepository.findByMemberId(payload.memberId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        if (!activeSession.matches(payload.sessionId())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
        Member member = memberRepository.findById(payload.memberId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        return new LoginMember(member.getId(), member.getName());
    }

    private void renewActiveSession(Member member, String sessionId) {
        ActiveSession activeSession = activeSessionRepository.findByMemberId(member.getId())
                .orElseGet(() -> new ActiveSession(member, sessionId));
        activeSession.renew(sessionId);
        activeSessionRepository.saveAndFlush(activeSession);
    }
}

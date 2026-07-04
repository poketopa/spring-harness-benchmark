package roomescape.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.AuthTokenProvider;
import roomescape.auth.AuthTokenProvider.TokenPayload;
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
        Member member = memberRepository.findWithLockByEmail(request.email())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.hasPassword(request.password())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String sessionKey = renewActiveSession(member);
        return new LoginResponse(authTokenProvider.createToken(member, sessionKey));
    }

    public LoginMember authenticate(String accessToken) {
        TokenPayload payload = authTokenProvider.extractPayload(accessToken);
        Member member = memberRepository.findById(payload.memberId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        ActiveSession activeSession = activeSessionRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        if (!activeSession.matches(payload.sessionKey())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
        return new LoginMember(member.getId(), member.getName());
    }

    private String renewActiveSession(Member member) {
        String sessionKey = authTokenProvider.createSessionKey();
        try {
            ActiveSession activeSession = activeSessionRepository.findByMemberId(member.getId())
                    .orElseGet(() -> new ActiveSession(member, sessionKey));
            activeSession.replaceWith(sessionKey);
            activeSessionRepository.saveAndFlush(activeSession);
            return sessionKey;
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "로그인을 다시 시도해 주세요.");
        }
    }
}

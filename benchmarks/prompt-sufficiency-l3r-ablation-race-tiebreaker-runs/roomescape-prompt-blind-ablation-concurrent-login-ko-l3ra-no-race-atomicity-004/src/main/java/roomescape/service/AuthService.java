package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.ActiveSessionStore;
import roomescape.auth.AuthTokenClaims;
import roomescape.auth.AuthTokenProvider;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.dto.LoginRequest;
import roomescape.dto.LoginResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final AuthTokenProvider authTokenProvider;
    private final ActiveSessionStore activeSessionStore;

    public AuthService(
            MemberRepository memberRepository,
            AuthTokenProvider authTokenProvider,
            ActiveSessionStore activeSessionStore
    ) {
        this.memberRepository = memberRepository;
        this.authTokenProvider = authTokenProvider;
        this.activeSessionStore = activeSessionStore;
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.hasPassword(request.password())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String sessionId = activeSessionStore.activate(member.getId());
        return new LoginResponse(authTokenProvider.createToken(member, sessionId));
    }

    public LoginMember authenticate(String accessToken) {
        AuthTokenClaims claims = authTokenProvider.extractClaims(accessToken);
        if (!activeSessionStore.isActive(claims)) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
        Member member = memberRepository.findById(claims.memberId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        return new LoginMember(member.getId(), member.getName());
    }
}

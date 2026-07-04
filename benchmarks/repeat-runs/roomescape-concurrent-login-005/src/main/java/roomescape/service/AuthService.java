package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.AuthTokenProvider;
import roomescape.auth.IssuedToken;
import roomescape.auth.LoginMember;
import roomescape.auth.TokenPayload;
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
        Member member = memberRepository.findByEmailForUpdate(request.email())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.hasPassword(request.password())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        IssuedToken issuedToken = authTokenProvider.issueToken(member);
        renewActiveSession(member, issuedToken.tokenId());
        return new LoginResponse(issuedToken.accessToken());
    }

    private void renewActiveSession(Member member, String tokenId) {
        AuthSession authSession = authSessionRepository.findByMemberId(member.getId())
                .orElseGet(() -> new AuthSession(member, tokenId));
        authSession.renew(tokenId);
        authSessionRepository.saveAndFlush(authSession);
    }

    public LoginMember authenticate(String accessToken) {
        TokenPayload payload = authTokenProvider.extractPayload(accessToken);
        validateActiveSession(payload);

        Member member = memberRepository.findById(payload.memberId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        return new LoginMember(member.getId(), member.getName());
    }

    private void validateActiveSession(TokenPayload payload) {
        if (!authSessionRepository.existsByMemberIdAndTokenId(payload.memberId(), payload.tokenId())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }
}

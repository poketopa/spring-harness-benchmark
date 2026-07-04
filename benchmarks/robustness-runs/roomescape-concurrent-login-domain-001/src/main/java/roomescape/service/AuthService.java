package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        Member member = findMemberByEmailForLogin(request.email());
        validatePassword(member, request.password());
        String accessToken = authTokenProvider.createToken(member);
        activateSession(member, accessToken);
        return new LoginResponse(accessToken);
    }

    private Member findMemberByEmailForLogin(String email) {
        return memberRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    private void validatePassword(Member member, String password) {
        if (!member.hasPassword(password)) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    private void activateSession(Member member, String accessToken) {
        AuthSession authSession = authSessionRepository.findByMember(member)
                .orElseGet(() -> new AuthSession(member, accessToken));
        authSession.renew(accessToken);
        authSessionRepository.saveAndFlush(authSession);
    }

    public LoginMember authenticate(String accessToken) {
        Long memberId = authTokenProvider.extractMemberId(accessToken);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        validateActiveSession(member, accessToken);
        return new LoginMember(member.getId(), member.getName());
    }

    private void validateActiveSession(Member member, String accessToken) {
        AuthSession authSession = authSessionRepository.findByMember(member)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        if (!authSession.hasAccessToken(accessToken)) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }
}

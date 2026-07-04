package roomescape.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.AuthTokenProvider;
import roomescape.auth.AuthTokenProvider.LoginToken;
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

    public AuthService(MemberRepository memberRepository, AuthTokenProvider authTokenProvider) {
        this.memberRepository = memberRepository;
        this.authTokenProvider = authTokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findWithLockByEmail(request.email())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!member.hasPassword(request.password())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        String loginKey = UUID.randomUUID().toString();
        member.renewLoginKey(loginKey);
        return new LoginResponse(authTokenProvider.createToken(member, loginKey));
    }

    public LoginMember authenticate(String accessToken) {
        LoginToken loginToken = authTokenProvider.extractLoginToken(accessToken);
        Member member = memberRepository.findById(loginToken.memberId())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
        if (!member.hasCurrentLoginKey(loginToken.loginKey())) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
        return new LoginMember(member.getId(), member.getName());
    }
}

package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
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

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthTokenProvider authTokenProvider;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("같은 계정으로 다시 로그인하면 현재 세션 키를 교체한다")
    void loginRotatesCurrentSession() {
        Member member = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        LoginRequest request = new LoginRequest("brown@example.com", "password");

        LoginResponse firstLogin = authService.login(request);
        LoginResponse secondLogin = authService.login(request);

        AuthToken firstToken = authTokenProvider.extractToken(firstLogin.accessToken());
        AuthToken secondToken = authTokenProvider.extractToken(secondLogin.accessToken());
        AuthSession currentSession = authSessionRepository.findByMember(member).orElseThrow();

        assertThat(firstToken.memberId()).isEqualTo(member.getId());
        assertThat(secondToken.memberId()).isEqualTo(member.getId());
        assertThat(firstToken.sessionKey()).isNotEqualTo(secondToken.sessionKey());
        assertThat(currentSession.matches(secondToken.sessionKey())).isTrue();
        assertThat(currentSession.matches(firstToken.sessionKey())).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 로그인하면 인증 예외가 발생한다")
    void loginWithUnknownEmailThrowsUnauthorized() {
        LoginRequest request = new LoginRequest("brown@example.com", "password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인하면 인증 예외가 발생한다")
    void loginWithWrongPasswordThrowsUnauthorized() {
        memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        LoginRequest request = new LoginRequest("brown@example.com", "wrong-password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("현재 로그인 토큰을 인증하면 로그인 회원 정보를 반환한다")
    void authenticateCurrentTokenReturnsLoginMember() {
        Member member = memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        LoginResponse login = authService.login(new LoginRequest("brown@example.com", "password"));

        LoginMember loginMember = authService.authenticate(login.accessToken());

        assertThat(loginMember.id()).isEqualTo(member.getId());
        assertThat(loginMember.name()).isEqualTo(member.getName());
    }

    @Test
    @DisplayName("이전 로그인 토큰을 인증하면 인증 예외가 발생한다")
    void authenticateOldTokenThrowsUnauthorized() {
        memberRepository.save(new Member("브라운", "brown@example.com", "password"));
        LoginRequest request = new LoginRequest("brown@example.com", "password");
        LoginResponse firstLogin = authService.login(request);
        authService.login(request);

        assertThatThrownBy(() -> authService.authenticate(firstLogin.accessToken()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}

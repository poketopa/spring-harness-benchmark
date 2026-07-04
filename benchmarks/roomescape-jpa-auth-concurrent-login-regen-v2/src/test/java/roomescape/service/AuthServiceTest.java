package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import roomescape.auth.AuthTokenProvider;
import roomescape.auth.LoginMember;
import roomescape.domain.AuthSession;
import roomescape.domain.Member;
import roomescape.dto.LoginRequest;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.AuthSessionRepository;
import roomescape.repository.MemberRepository;

class AuthServiceTest {

    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final AuthSessionRepository authSessionRepository = mock(AuthSessionRepository.class);
    private final AuthTokenProvider authTokenProvider = mock(AuthTokenProvider.class);
    private final AuthService authService = new AuthService(
            memberRepository,
            authSessionRepository,
            authTokenProvider
    );

    @Test
    @DisplayName("다시 로그인하면 기존 인증 세션의 토큰을 새 토큰으로 교체한다")
    void loginRenewsActiveSession() {
        // given
        Member member = memberWithId(1L);
        AuthSession authSession = new AuthSession(member, "old-token");
        LoginRequest request = new LoginRequest("brown@example.com", "password");
        when(memberRepository.findByEmailForUpdate("brown@example.com")).thenReturn(Optional.of(member));
        when(authTokenProvider.createToken(member)).thenReturn("new-token");
        when(authSessionRepository.findByMember(member)).thenReturn(Optional.of(authSession));

        // when
        authService.login(request);

        // then
        assertThat(authSession.hasAccessToken("old-token")).isFalse();
        assertThat(authSession.hasAccessToken("new-token")).isTrue();
    }

    @Test
    @DisplayName("활성 토큰이 아니면 인증에 실패한다")
    void authenticateWithExpiredTokenThrowsUnauthorized() {
        // given
        Member member = memberWithId(1L);
        AuthSession authSession = new AuthSession(member, "new-token");
        when(authTokenProvider.extractMemberId("old-token")).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(authSessionRepository.findByMember(member)).thenReturn(Optional.of(authSession));

        // when & then
        assertThatThrownBy(() -> authService.authenticate("old-token"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("활성 토큰이면 로그인 회원을 반환한다")
    void authenticateWithActiveToken() {
        // given
        Member member = memberWithId(1L);
        AuthSession authSession = new AuthSession(member, "active-token");
        when(authTokenProvider.extractMemberId("active-token")).thenReturn(1L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));
        when(authSessionRepository.findByMember(member)).thenReturn(Optional.of(authSession));

        // when
        LoginMember loginMember = authService.authenticate("active-token");

        // then
        assertThat(loginMember.id()).isEqualTo(1L);
        assertThat(loginMember.name()).isEqualTo("브라운");
    }

    private Member memberWithId(Long id) {
        Member member = new Member("브라운", "brown@example.com", "password");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}

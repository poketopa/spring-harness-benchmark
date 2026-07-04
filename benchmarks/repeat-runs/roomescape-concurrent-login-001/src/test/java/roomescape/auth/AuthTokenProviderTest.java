package roomescape.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class AuthTokenProviderTest {

    private final AuthTokenProvider authTokenProvider = new AuthTokenProvider();

    @Test
    @DisplayName("같은 회원과 세션으로도 토큰은 고유하게 발급된다")
    void createUniqueTokens() {
        Member member = memberWithId(1L);

        String first = authTokenProvider.createToken(member, "session-key");
        String second = authTokenProvider.createToken(member, "session-key");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("토큰에서 회원 ID와 세션 키를 추출한다")
    void extractMemberIdAndSessionKey() {
        Member member = memberWithId(1L);
        String token = authTokenProvider.createToken(member, "session-key");

        Long memberId = authTokenProvider.extractMemberId(token);
        String sessionKey = authTokenProvider.extractSessionKey(token);

        assertThat(memberId).isEqualTo(1L);
        assertThat(sessionKey).isEqualTo("session-key");
    }

    @Test
    @DisplayName("잘못된 토큰은 인증 예외로 처리한다")
    void invalidTokenThrowsUnauthorized() {
        assertThatThrownBy(() -> authTokenProvider.extractMemberId("invalid"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private Member memberWithId(Long id) {
        Member member = new Member("브라운", "brown@example.com", "password");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}

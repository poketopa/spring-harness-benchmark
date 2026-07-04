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
    @DisplayName("같은 회원에게 로그인마다 다른 토큰을 발급하고 회원 ID를 복원한다")
    void createDifferentTokensAndExtractMemberId() {
        Member member = memberWithId(1L);

        String firstToken = authTokenProvider.createToken(member);
        String secondToken = authTokenProvider.createToken(member);

        assertThat(firstToken).isNotEqualTo(secondToken);
        assertThat(authTokenProvider.extractMemberId(firstToken)).isEqualTo(1L);
        assertThat(authTokenProvider.extractMemberId(secondToken)).isEqualTo(1L);
    }

    @Test
    @DisplayName("잘못된 토큰이면 인증 실패 예외를 던진다")
    void invalidTokenThrowsUnauthorized() {
        assertThatThrownBy(() -> authTokenProvider.extractMemberId("invalid-token"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private Member memberWithId(Long id) {
        Member member = new Member("브라운", "brown@example.com", "password");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }
}

package roomescape.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import roomescape.auth.AuthTokenProvider.AuthToken;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class AuthTokenProviderTest {

    private final AuthTokenProvider authTokenProvider = new AuthTokenProvider();

    @Test
    @DisplayName("회원 ID와 로그인 세션 키가 포함된 토큰을 생성하고 추출한다")
    void createAndExtractToken() {
        Member member = new Member("브라운", "brown@example.com", "password");
        ReflectionTestUtils.setField(member, "id", 1L);
        member.renewLoginSession("session-key");

        String token = authTokenProvider.createToken(member);
        AuthToken authToken = authTokenProvider.extractToken(token);

        assertThat(authToken.memberId()).isEqualTo(1L);
        assertThat(authToken.loginSessionKey()).isEqualTo("session-key");
    }

    @Test
    @DisplayName("로그인 세션 키는 매번 새로 생성된다")
    void createUniqueLoginSessionKey() {
        String firstKey = authTokenProvider.createLoginSessionKey();
        String secondKey = authTokenProvider.createLoginSessionKey();

        assertThat(firstKey).isNotBlank();
        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    @Test
    @DisplayName("잘못된 토큰은 인증 예외가 발생한다")
    void invalidTokenThrowsUnauthorized() {
        assertThatThrownBy(() -> authTokenProvider.extractToken("invalid-token"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}

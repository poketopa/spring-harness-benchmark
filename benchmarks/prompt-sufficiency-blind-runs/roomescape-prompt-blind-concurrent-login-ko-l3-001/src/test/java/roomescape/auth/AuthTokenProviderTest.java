package roomescape.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class AuthTokenProviderTest {

    private final AuthTokenProvider authTokenProvider = new AuthTokenProvider();

    @Test
    @DisplayName("회원 id와 로그인 토큰 식별자를 담은 인증 토큰을 생성하고 추출한다")
    void createsAndExtractsAccessTokenPayload() throws Exception {
        Member member = new Member("브라운", "brown@example.com", "password");
        setId(member, 1L);

        String token = authTokenProvider.createToken(member, "login-token-id");

        AccessTokenPayload payload = authTokenProvider.extractPayload(token);
        assertThat(payload.memberId()).isEqualTo(1L);
        assertThat(payload.loginTokenId()).isEqualTo("login-token-id");
    }

    @Test
    @DisplayName("잘못된 인증 토큰은 거부한다")
    void rejectsInvalidAccessToken() {
        assertThatThrownBy(() -> authTokenProvider.extractPayload("invalid-token"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private void setId(Member member, Long id) throws Exception {
        Field idField = Member.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(member, id);
    }
}

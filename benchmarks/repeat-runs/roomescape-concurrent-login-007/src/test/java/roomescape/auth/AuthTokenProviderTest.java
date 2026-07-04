package roomescape.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.RoomescapeException;

class AuthTokenProviderTest {

    private final AuthTokenProvider authTokenProvider = new AuthTokenProvider();

    @Test
    @DisplayName("같은 회원에게 발급한 토큰도 매번 다른 값이다")
    void createDistinctTokens() {
        String sessionKey = authTokenProvider.createSessionKey();

        String first = authTokenProvider.createToken(1L, sessionKey);
        String second = authTokenProvider.createToken(1L, sessionKey);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("토큰에서 로그인 회원 식별자와 세션 키를 추출한다")
    void extractPayload() {
        String sessionKey = authTokenProvider.createSessionKey();
        String token = authTokenProvider.createToken(1L, sessionKey);

        TokenPayload payload = authTokenProvider.extractPayload(token);

        assertThat(payload.memberId()).isEqualTo(1L);
        assertThat(payload.sessionKey()).isEqualTo(sessionKey);
    }

    @Test
    @DisplayName("형식이 올바르지 않은 토큰은 인증 실패로 처리한다")
    void rejectInvalidToken() {
        assertThatThrownBy(() -> authTokenProvider.extractPayload("invalid"))
                .isInstanceOf(RoomescapeException.class);
    }
}

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
    @DisplayName("토큰에서 회원 ID와 세션 키를 추출한다")
    void extractPayload() {
        Member member = new Member("브라운", "brown@example.com", "password");
        ReflectionTestUtils.setField(member, "id", 1L);
        String sessionKey = authTokenProvider.createSessionKey();

        String token = authTokenProvider.createToken(member, sessionKey);

        AuthTokenProvider.TokenPayload payload = authTokenProvider.extractPayload(token);
        assertThat(payload.memberId()).isEqualTo(1L);
        assertThat(payload.sessionKey()).isEqualTo(sessionKey);
    }

    @Test
    @DisplayName("세션 키를 새로 만들면 서로 다른 값이 나온다")
    void createUniqueSessionKeys() {
        String first = authTokenProvider.createSessionKey();
        String second = authTokenProvider.createSessionKey();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 인증 예외를 던진다")
    void invalidTokenThrowsUnauthorized() {
        assertThatThrownBy(() -> authTokenProvider.extractPayload("invalid-token"))
                .isInstanceOf(RoomescapeException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}

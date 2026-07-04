package roomescape.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class AuthTokenProviderTest {

    private final AuthTokenProvider authTokenProvider = new AuthTokenProvider();

    @Test
    @DisplayName("같은 회원에게 발급한 토큰은 매번 다른 토큰 식별자를 가진다")
    void issueUniqueToken() {
        IssuedToken first = authTokenProvider.issueToken(1L);
        IssuedToken second = authTokenProvider.issueToken(1L);

        assertThat(first.accessToken()).isNotEqualTo(second.accessToken());
        assertThat(first.tokenId()).isNotEqualTo(second.tokenId());
        assertThat(authTokenProvider.extractPayload(first.accessToken()))
                .isEqualTo(new TokenPayload(1L, first.tokenId()));
    }

    @Test
    @DisplayName("잘못된 토큰은 인증 실패 예외로 변환한다")
    void invalidToken() {
        assertThatThrownBy(() -> authTokenProvider.extractPayload("invalid-token"))
                .isInstanceOf(RoomescapeException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
}

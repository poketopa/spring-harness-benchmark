package roomescape.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class AuthTokenProviderTest {

    private final AuthTokenProvider authTokenProvider = new AuthTokenProvider();

    @Test
    @DisplayName("토큰에서 회원 ID와 세션 키를 추출한다")
    void extractPayload() {
        Member member = new Member("브라운", "brown@example.com", "password");
        String sessionKey = "session-key";
        String token = authTokenProvider.createToken(memberWithId(1L), sessionKey);

        AuthTokenProvider.AuthTokenPayload payload = authTokenProvider.extractPayload(token);

        assertThat(payload.memberId()).isEqualTo(1L);
        assertThat(payload.sessionKey()).isEqualTo(sessionKey);
    }

    @Test
    @DisplayName("잘못된 토큰은 인증 실패 예외를 발생시킨다")
    void invalidTokenThrowsUnauthorized() {
        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString("invalid".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> authTokenProvider.extractPayload(token))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private Member memberWithId(Long id) {
        Member member = new Member("브라운", "brown@example.com", "password");
        TestMemberIdSetter.setId(member, id);
        return member;
    }
}

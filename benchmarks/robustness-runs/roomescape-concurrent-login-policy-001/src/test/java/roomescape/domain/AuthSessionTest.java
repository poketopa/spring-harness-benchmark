package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class AuthSessionTest {

    @Test
    @DisplayName("인증 세션은 활성 토큰을 교체할 수 있다")
    void renewAccessToken() {
        Member member = new Member("브라운", "brown@example.com", "password");
        AuthSession authSession = new AuthSession(member, "old-token");

        authSession.renew("new-token");

        assertThat(authSession.hasAccessToken("old-token")).isFalse();
        assertThat(authSession.hasAccessToken("new-token")).isTrue();
    }

    @Test
    @DisplayName("인증 세션은 회원과 토큰이 필요하다")
    void createWithoutRequiredValuesThrowsInvalidInput() {
        Member member = new Member("브라운", "brown@example.com", "password");

        assertThatThrownBy(() -> new AuthSession(null, "token"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new AuthSession(member, " "))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}

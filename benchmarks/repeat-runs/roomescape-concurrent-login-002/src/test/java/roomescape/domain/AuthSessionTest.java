package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthSessionTest {

    @Test
    @DisplayName("인증 세션 키를 새 값으로 교체한다")
    void rotateSessionKey() {
        Member member = new Member("브라운", "brown@example.com", "password");
        AuthSession authSession = new AuthSession(member, "old-session");

        authSession.rotate("new-session");

        assertThat(authSession.matches("old-session")).isFalse();
        assertThat(authSession.matches("new-session")).isTrue();
    }
}

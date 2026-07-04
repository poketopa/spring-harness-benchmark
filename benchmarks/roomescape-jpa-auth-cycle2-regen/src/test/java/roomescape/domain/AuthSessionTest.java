package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthSessionTest {

    @Test
    @DisplayName("세션 키가 현재 로그인 세션과 일치하는지 판단한다")
    void sessionKeyIsMatched() {
        Member member = new Member("브라운", "brown@example.com", "password");
        AuthSession session = new AuthSession(member, "old-session");

        session.rotate("new-session");

        assertThat(session.matches("new-session")).isTrue();
        assertThat(session.matches("old-session")).isFalse();
    }
}

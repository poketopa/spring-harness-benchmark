package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.RoomescapeException;

class AuthSessionTest {

    @Test
    @DisplayName("세션 키를 갱신하면 이전 세션 키는 일치하지 않는다")
    void renewSessionKey() {
        AuthSession session = new AuthSession(new Member("브라운", "brown@example.com", "password"), "first");

        session.renew("second");

        assertThat(session.matches("first")).isFalse();
        assertThat(session.matches("second")).isTrue();
    }

    @Test
    @DisplayName("세션 회원과 세션 키는 비어 있을 수 없다")
    void validateRequiredValues() {
        assertThatThrownBy(() -> new AuthSession(null, "session-key"))
                .isInstanceOf(RoomescapeException.class);
        assertThatThrownBy(() -> new AuthSession(new Member("브라운", "brown@example.com", "password"), " "))
                .isInstanceOf(RoomescapeException.class);
    }
}

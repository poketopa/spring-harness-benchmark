package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class MemberTest {

    @Test
    @DisplayName("회원 이름, 이메일, 비밀번호는 비어 있을 수 없다")
    void blankMemberFieldsAreRejected() {
        assertThatThrownBy(() -> new Member("", "brown@example.com", "password"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Member("브라운", " ", "password"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Member("브라운", "brown@example.com", null))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("회원 권한은 비어 있을 수 없다")
    void nullRoleIsRejected() {
        assertThatThrownBy(() -> new Member("브라운", "brown@example.com", "password", null))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("회원 권한을 판단한다")
    void memberRoleIsDetected() {
        Member user = new Member("브라운", "brown@example.com", "password");
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);
        Member admin = new Member("어드민", "admin@example.com", "password", Role.ADMIN);

        assertThat(user.isManager()).isFalse();
        assertThat(user.isAdmin()).isFalse();
        assertThat(manager.isManager()).isTrue();
        assertThat(admin.isAdmin()).isTrue();
    }
}

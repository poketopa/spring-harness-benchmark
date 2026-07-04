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
    @DisplayName("비밀번호가 일치하는지 판단한다")
    void passwordMatchesRawPassword() {
        Member member = new Member("브라운", "brown@example.com", "password");

        assertThat(member.hasPassword("password")).isTrue();
        assertThat(member.hasPassword("wrong-password")).isFalse();
    }

    @Test
    @DisplayName("현재 로그인 토큰 식별자를 갱신하고 일치 여부를 판단한다")
    void loginTokenCanBeRenewed() {
        Member member = new Member("브라운", "brown@example.com", "password");

        member.renewLoginToken("first-token");
        member.renewLoginToken("second-token");

        assertThat(member.hasLoginToken("first-token")).isFalse();
        assertThat(member.hasLoginToken("second-token")).isTrue();
    }
}

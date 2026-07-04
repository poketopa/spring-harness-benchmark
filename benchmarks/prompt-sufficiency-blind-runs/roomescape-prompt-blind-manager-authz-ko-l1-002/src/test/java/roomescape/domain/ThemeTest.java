package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class ThemeTest {

    @Test
    @DisplayName("테마를 생성한다")
    void createTheme() {
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");

        assertThat(theme.getName()).isEqualTo("어둠의 방");
        assertThat(theme.getDescription()).isEqualTo("방탈출");
        assertThat(theme.getThumbnailUrl()).isEqualTo("https://example.com/dark.jpg");
    }

    @Test
    @DisplayName("테마 이름, 설명, 썸네일은 비어 있을 수 없다")
    void blankThemeFieldsAreRejected() {
        assertThatThrownBy(() -> new Theme("", "방탈출", "https://example.com/dark.jpg"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Theme("어둠의 방", " ", "https://example.com/dark.jpg"))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Theme("어둠의 방", "방탈출", null))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}

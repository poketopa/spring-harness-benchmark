package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class ThemeTest {

    @Test
    @DisplayName("테마가 속한 매장의 매니저인지 판단한다")
    void managedThemeIsDetectedByMember() {
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);
        Store store = new Store("강남점", manager);
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg", store);

        assertThat(theme.isManagedBy(manager)).isTrue();
    }

    @Test
    @DisplayName("테마가 속한 매장의 매니저가 아니면 false를 반환한다")
    void differentManagerIsNotThemeManager() {
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);
        Member otherManager = new Member("다른매니저", "other-manager@example.com", "password", Role.MANAGER);
        Store store = new Store("강남점", manager);
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg", store);

        assertThat(theme.isManagedBy(otherManager)).isFalse();
    }

    @Test
    @DisplayName("매장에 속하지 않은 테마는 관리 매니저가 없다")
    void themeWithoutStoreHasNoManager() {
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");

        assertThat(theme.isManagedBy(manager)).isFalse();
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

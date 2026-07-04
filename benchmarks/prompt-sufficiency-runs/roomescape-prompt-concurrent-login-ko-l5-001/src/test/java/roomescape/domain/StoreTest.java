package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class StoreTest {

    @Test
    @DisplayName("매니저 권한 회원으로 매장을 생성할 수 있다")
    void createStoreWithManager() {
        Member manager = new Member("브라운", "brown@example.com", "password", Role.MANAGER);

        Store store = new Store("강남점", manager);

        assertThat(store.getName()).isEqualTo("강남점");
        assertThat(store.isManagedBy(manager)).isTrue();
    }

    @Test
    @DisplayName("일반 회원으로 매장을 생성할 수 없다")
    void createStoreWithUserThrowsInvalidInput() {
        Member user = new Member("브라운", "brown@example.com", "password");

        assertThatThrownBy(() -> new Store("강남점", user))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}

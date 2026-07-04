package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class StoreTest {

    @Test
    @DisplayName("매장은 매니저 회원만 관리할 수 있다")
    void storeRequiresManager() {
        Member user = new Member("브라운", "brown@example.com", "password");

        assertThatThrownBy(() -> new Store("강남점", user))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("매장은 자신을 관리하는 매니저를 판단할 수 있다")
    void storeKnowsManager() {
        Member manager = new Member("코니", "cony@example.com", "password", Role.MANAGER);
        Store store = new Store("강남점", manager);

        assertThat(store.isManagedBy(manager)).isTrue();
        assertThat(store.isManagedBy(new Member("샐리", "sally@example.com", "password", Role.MANAGER))).isFalse();
    }
}

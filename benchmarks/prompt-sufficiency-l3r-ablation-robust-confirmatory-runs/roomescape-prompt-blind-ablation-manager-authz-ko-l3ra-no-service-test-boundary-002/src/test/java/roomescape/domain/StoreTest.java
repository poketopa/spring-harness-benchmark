package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class StoreTest {

    @Test
    @DisplayName("매장은 매니저 권한 회원이 관리한다")
    void createStoreWithManager() {
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);

        Store store = new Store("강남점", manager);

        assertThat(store.getName()).isEqualTo("강남점");
        assertThat(store.isManagedBy(manager)).isTrue();
    }

    @Test
    @DisplayName("매장 매니저는 매니저 권한 회원이어야 한다")
    void storeManagerMustHaveManagerRole() {
        Member user = new Member("브라운", "brown@example.com", "password");

        assertThatThrownBy(() -> new Store("강남점", user))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}

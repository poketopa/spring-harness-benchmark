package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class StoreTest {

    @Test
    @DisplayName("매장은 매니저 권한 회원을 관리자로 가진다")
    void createStoreWithManager() {
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);

        Store store = new Store("강남점", manager);

        assertThat(store.getManager()).isEqualTo(manager);
        assertThat(store.isManagedBy(manager)).isTrue();
    }

    @Test
    @DisplayName("매니저가 아닌 회원은 매장 관리자가 될 수 없다")
    void storeManagerMustHaveManagerRole() {
        Member user = new Member("회원", "user@example.com", "password", Role.USER);

        assertThatThrownBy(() -> new Store("강남점", user))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}

package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class StoreTest {

    @Test
    @DisplayName("매장 관리자를 판단한다")
    void storeKnowsItsManager() {
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);
        Store store = new Store("잠실점", manager);

        assertThat(store.isManagedBy(manager)).isTrue();
    }

    @Test
    @DisplayName("일반 회원은 매장 관리자가 될 수 없다")
    void nonManagerCannotManageStore() {
        Member member = new Member("브라운", "brown@example.com", "password");

        assertThatThrownBy(() -> new Store("잠실점", member))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}

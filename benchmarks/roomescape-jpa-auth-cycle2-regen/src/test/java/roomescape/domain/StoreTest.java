package roomescape.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

class StoreTest {

    @Test
    @DisplayName("매장 관리자인지 판단한다")
    void managedStoreIsDetectedByMember() {
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);
        Store store = new Store("강남점", manager);

        assertThat(store.isManagedBy(manager)).isTrue();
    }

    @Test
    @DisplayName("매장 관리자가 아니면 false를 반환한다")
    void differentManagerIsNotStoreManager() {
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);
        Member otherManager = new Member("다른매니저", "other-manager@example.com", "password", Role.MANAGER);
        Store store = new Store("강남점", manager);

        assertThat(store.isManagedBy(otherManager)).isFalse();
    }

    @Test
    @DisplayName("매장 이름과 관리자는 비어 있을 수 없다")
    void blankNameAndNullManagerAreRejected() {
        Member manager = new Member("매니저", "manager@example.com", "password", Role.MANAGER);

        assertThatThrownBy(() -> new Store("", manager))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThatThrownBy(() -> new Store("강남점", null))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("매니저가 아닌 회원은 매장을 관리할 수 없다")
    void nonManagerCannotOwnStore() {
        Member member = new Member("브라운", "brown@example.com", "password");

        assertThatThrownBy(() -> new Store("강남점", member))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}

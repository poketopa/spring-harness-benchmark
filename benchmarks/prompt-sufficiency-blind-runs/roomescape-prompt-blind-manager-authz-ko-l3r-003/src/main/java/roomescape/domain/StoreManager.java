package roomescape.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
@Table(
        name = "store_managers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_store_manager_member_store",
                        columnNames = {"manager_id", "store_id"}
                )
        }
)
public class StoreManager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id")
    private Member manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    protected StoreManager() {
    }

    public StoreManager(Member manager, Store store) {
        validateRequired(manager, store);
        this.manager = manager;
        this.store = store;
    }

    private void validateRequired(Member manager, Store store) {
        if (manager == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매니저 회원은 비어 있을 수 없습니다.");
        }
        if (!manager.isManager()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매니저 권한 회원만 매장을 관리할 수 있습니다.");
        }
        if (store == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "관리 매장은 비어 있을 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public Member getManager() {
        return manager;
    }

    public Store getStore() {
        return store;
    }
}

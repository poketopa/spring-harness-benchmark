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
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_manager_store_manager_store",
                columnNames = {"manager_id", "store_id"}
        )
})
public class ManagerStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id")
    private Member manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id")
    private Store store;

    protected ManagerStore() {
    }

    public ManagerStore(Member manager, Store store) {
        validateRequired(manager, store);
        this.manager = manager;
        this.store = store;
    }

    private void validateRequired(Member manager, Store store) {
        if (manager == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매니저는 비어 있을 수 없습니다.");
        }
        if (!manager.isManager()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매니저 권한이 필요합니다.");
        }
        if (store == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매장은 비어 있을 수 없습니다.");
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

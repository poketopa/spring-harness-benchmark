package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    protected Store() {
    }

    public Store(String name) {
        validateName(name);
        this.name = name;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매장 이름은 비어 있을 수 없습니다.");
        }
    }

    public boolean isSameStore(Store targetStore) {
        if (targetStore == null) {
            return false;
        }
        if (this == targetStore) {
            return true;
        }
        Long storeId = getId();
        Long targetStoreId = targetStore.getId();
        return storeId != null && storeId.equals(targetStoreId);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

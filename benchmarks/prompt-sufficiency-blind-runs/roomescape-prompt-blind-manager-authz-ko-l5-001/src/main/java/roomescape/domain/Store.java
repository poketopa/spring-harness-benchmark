package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    protected Store() {
    }

    public Store(String name) {
        validateNonBlank(name);
        this.name = name;
    }

    public static Store defaultStore() {
        return new Store("기본 매장");
    }

    private void validateNonBlank(String value) {
        if (value == null || value.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매장 이름은 비어 있을 수 없습니다.");
        }
    }

    public boolean isSameStore(Store store) {
        if (this == store) {
            return true;
        }
        return id != null && store != null && id.equals(store.id);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

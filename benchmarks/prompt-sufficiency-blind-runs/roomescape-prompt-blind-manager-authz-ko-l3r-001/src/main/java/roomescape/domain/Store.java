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

    public static final String DEFAULT_NAME = "기본 매장";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
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

    public boolean isSameAs(Store target) {
        if (this == target) {
            return true;
        }
        return target != null && id != null && id.equals(target.getId());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}

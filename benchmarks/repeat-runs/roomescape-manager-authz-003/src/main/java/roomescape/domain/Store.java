package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_store_manager", columnNames = "manager_id")
})
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id")
    private Member manager;

    protected Store() {
    }

    public Store(String name, Member manager) {
        validateRequired(name, manager);
        this.name = name;
        this.manager = manager;
    }

    private void validateRequired(String name, Member manager) {
        if (name == null || name.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매장 이름은 비어 있을 수 없습니다.");
        }
        if (manager == null || manager.getRole() != Role.MANAGER) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매장 관리자는 매니저 권한이어야 합니다.");
        }
    }

    public boolean isManagedBy(Member member) {
        if (manager == member) {
            return true;
        }
        return manager.getId() != null && manager.getId().equals(member.getId());
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Member getManager() {
        return manager;
    }
}

package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Member manager;

    protected Store() {
    }

    public Store(String name) {
        this(name, null);
    }

    public Store(String name, Member manager) {
        validateName(name);
        validateManager(manager);
        this.name = name;
        this.manager = manager;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매장 이름은 비어 있을 수 없습니다.");
        }
    }

    private void validateManager(Member manager) {
        if (manager != null && !manager.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매장 매니저만 매장을 관리할 수 있습니다.");
        }
    }

    public boolean isManagedBy(Member member) {
        if (manager == null || member == null || !member.isManager()) {
            return false;
        }
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

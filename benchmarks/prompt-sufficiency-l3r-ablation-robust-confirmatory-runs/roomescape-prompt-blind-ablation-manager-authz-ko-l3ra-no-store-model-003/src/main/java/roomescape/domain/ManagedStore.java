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
                name = "uk_managed_store_manager_theme",
                columnNames = {"manager_id", "theme_id"}
        )
})
public class ManagedStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id")
    private Member manager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "theme_id")
    private Theme theme;

    protected ManagedStore() {
    }

    public ManagedStore(Member manager, Theme theme) {
        validateRequired(manager, theme);
        this.manager = manager;
        this.theme = theme;
    }

    private void validateRequired(Member manager, Theme theme) {
        if (manager == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매장 매니저는 비어 있을 수 없습니다.");
        }
        if (!manager.isManager()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "매장 매니저 권한이 필요합니다.");
        }
        if (theme == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "관리 매장은 비어 있을 수 없습니다.");
        }
    }

    public Long getId() {
        return id;
    }

    public Member getManager() {
        return manager;
    }

    public Theme getTheme() {
        return theme;
    }
}

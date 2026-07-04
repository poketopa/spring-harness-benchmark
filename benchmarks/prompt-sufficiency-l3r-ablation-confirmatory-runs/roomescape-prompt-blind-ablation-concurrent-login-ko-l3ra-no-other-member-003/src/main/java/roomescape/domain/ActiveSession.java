package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "active_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_active_sessions_member_id", columnNames = "member_id")
)
public class ActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private String sessionId;

    protected ActiveSession() {
    }

    public ActiveSession(Long memberId, String sessionId) {
        this.memberId = memberId;
        this.sessionId = sessionId;
    }

    public void renew(String sessionId) {
        this.sessionId = sessionId;
    }

    public boolean matches(String sessionId) {
        return this.sessionId.equals(sessionId);
    }
}

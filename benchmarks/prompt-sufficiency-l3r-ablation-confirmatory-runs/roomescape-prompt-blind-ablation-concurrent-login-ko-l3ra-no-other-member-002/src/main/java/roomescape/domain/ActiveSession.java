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

@Entity
@Table(
        name = "active_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_active_session_member", columnNames = "member_id")
        }
)
public class ActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true)
    private String sessionKey;

    protected ActiveSession() {
    }

    public ActiveSession(Member member, String sessionKey) {
        this.member = member;
        this.sessionKey = sessionKey;
    }

    public void replaceWith(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public boolean matches(String sessionKey) {
        return this.sessionKey.equals(sessionKey);
    }
}

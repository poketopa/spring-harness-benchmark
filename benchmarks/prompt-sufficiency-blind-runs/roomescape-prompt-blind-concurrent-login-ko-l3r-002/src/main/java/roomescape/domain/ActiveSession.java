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

@Entity
@Table(name = "active_sessions")
public class ActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "session_key", nullable = false, unique = true)
    private String sessionKey;

    protected ActiveSession() {
    }

    public ActiveSession(Member member, String sessionKey) {
        this.member = member;
        this.sessionKey = sessionKey;
    }

    public void changeSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }
}

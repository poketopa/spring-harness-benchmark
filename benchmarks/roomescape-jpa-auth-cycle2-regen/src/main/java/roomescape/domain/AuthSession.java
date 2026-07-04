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
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_session_member", columnNames = "member_id"),
        @UniqueConstraint(name = "uk_auth_session_key", columnNames = "session_key")
})
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "session_key", nullable = false)
    private String sessionKey;

    protected AuthSession() {
    }

    public AuthSession(Member member, String sessionKey) {
        this.member = member;
        this.sessionKey = sessionKey;
    }

    public void rotate(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public boolean matches(String sessionKey) {
        return this.sessionKey.equals(sessionKey);
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public String getSessionKey() {
        return sessionKey;
    }
}

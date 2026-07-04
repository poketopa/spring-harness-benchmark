package roomescape.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "active_sessions")
public class ActiveSession {

    @Id
    private Long memberId;

    @Column(nullable = false, unique = true)
    private String token;

    protected ActiveSession() {
    }

    public ActiveSession(Long memberId, String token) {
        this.memberId = memberId;
        this.token = token;
    }

    public void renew(String token) {
        this.token = token;
    }

    public boolean matches(String token) {
        return this.token.equals(token);
    }
}

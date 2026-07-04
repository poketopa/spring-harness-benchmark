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
import java.util.UUID;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Entity
@Table(name = "active_sessions")
public class ActiveSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, unique = true)
    private String sessionId;

    protected ActiveSession() {
    }

    public ActiveSession(Member member) {
        validateMember(member);
        this.member = member;
        this.sessionId = newSessionId();
    }

    public void renew() {
        sessionId = newSessionId();
    }

    public boolean matches(String targetSessionId) {
        return sessionId.equals(targetSessionId);
    }

    private void validateMember(Member member) {
        if (member == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "세션 회원은 비어 있을 수 없습니다.");
        }
    }

    private String newSessionId() {
        return UUID.randomUUID().toString();
    }

    public String getSessionId() {
        return sessionId;
    }
}

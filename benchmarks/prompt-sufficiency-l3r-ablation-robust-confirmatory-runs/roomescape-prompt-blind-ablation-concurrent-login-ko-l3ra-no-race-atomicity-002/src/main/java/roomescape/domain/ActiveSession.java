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

    public ActiveSession(Member member, String sessionId) {
        validateRequired(member, sessionId);
        this.member = member;
        this.sessionId = sessionId;
    }

    public void renew(String sessionId) {
        validateSessionId(sessionId);
        this.sessionId = sessionId;
    }

    public boolean matches(String sessionId) {
        return this.sessionId.equals(sessionId);
    }

    private void validateRequired(Member member, String sessionId) {
        if (member == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "세션 회원은 비어 있을 수 없습니다.");
        }
        validateSessionId(sessionId);
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "세션 식별자는 비어 있을 수 없습니다.");
        }
    }
}

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
@Table(
        name = "login_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_login_session_member", columnNames = "member_id"),
                @UniqueConstraint(name = "uk_login_session_key", columnNames = "session_key")
        }
)
public class LoginSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "session_key", nullable = false)
    private String sessionKey;

    protected LoginSession() {
    }

    public LoginSession(Member member, String sessionKey) {
        validateRequired(member, sessionKey);
        this.member = member;
        this.sessionKey = sessionKey;
    }

    private void validateRequired(Member member, String sessionKey) {
        if (member == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "로그인 세션 회원은 비어 있을 수 없습니다.");
        }
        validateSessionKey(sessionKey);
    }

    private void validateSessionKey(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "로그인 세션 키는 비어 있을 수 없습니다.");
        }
    }

    public void renew(String sessionKey) {
        validateSessionKey(sessionKey);
        this.sessionKey = sessionKey;
    }

    public boolean hasSessionKey(String targetSessionKey) {
        return sessionKey.equals(targetSessionKey);
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

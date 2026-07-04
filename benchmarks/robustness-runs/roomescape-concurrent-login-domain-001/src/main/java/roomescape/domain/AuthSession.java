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
        name = "auth_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_session_member", columnNames = "member_id")
        }
)
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(nullable = false, unique = true)
    private String accessToken;

    protected AuthSession() {
    }

    public AuthSession(Member member, String accessToken) {
        validateRequired(member, accessToken);
        this.member = member;
        this.accessToken = accessToken;
    }

    private void validateRequired(Member member, String accessToken) {
        if (member == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "인증 세션 회원은 비어 있을 수 없습니다.");
        }
        validateAccessToken(accessToken);
    }

    private void validateAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "인증 토큰은 비어 있을 수 없습니다.");
        }
    }

    public void renew(String accessToken) {
        validateAccessToken(accessToken);
        this.accessToken = accessToken;
    }

    public boolean hasAccessToken(String accessToken) {
        return this.accessToken.equals(accessToken);
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public String getAccessToken() {
        return accessToken;
    }
}

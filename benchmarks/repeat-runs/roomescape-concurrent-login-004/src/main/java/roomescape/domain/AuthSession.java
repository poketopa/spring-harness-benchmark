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
                @UniqueConstraint(name = "uk_auth_sessions_member", columnNames = "member_id"),
                @UniqueConstraint(name = "uk_auth_sessions_access_token", columnNames = "access_token")
        }
)
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "access_token", nullable = false, unique = true, length = 512)
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

    public boolean isActiveToken(String accessToken) {
        return this.accessToken.equals(accessToken);
    }
}

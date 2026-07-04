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
                @UniqueConstraint(name = "uk_auth_sessions_token", columnNames = "token_id")
        }
)
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "token_id", nullable = false, unique = true)
    private String tokenId;

    protected AuthSession() {
    }

    public AuthSession(Member member, String tokenId) {
        validateRequired(member, tokenId);
        this.member = member;
        this.tokenId = tokenId;
    }

    private void validateRequired(Member member, String tokenId) {
        if (member == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "인증 세션 회원은 비어 있을 수 없습니다.");
        }
        validateTokenId(tokenId);
    }

    private void validateTokenId(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "인증 세션 토큰은 비어 있을 수 없습니다.");
        }
    }

    public void renew(String tokenId) {
        validateTokenId(tokenId);
        this.tokenId = tokenId;
    }
}

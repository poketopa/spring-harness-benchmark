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

    @Column(nullable = false)
    private String accessToken;

    protected ActiveSession() {
    }

    public ActiveSession(Member member, String accessToken) {
        validateRequired(member, accessToken);
        this.member = member;
        this.accessToken = accessToken;
    }

    private void validateRequired(Member member, String accessToken) {
        if (member == null) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "세션 회원은 비어 있을 수 없습니다.");
        }
        validateNonBlank(accessToken);
    }

    private void validateNonBlank(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new RoomescapeException(ErrorCode.INVALID_INPUT, "세션 토큰은 비어 있을 수 없습니다.");
        }
    }

    public void rotate(String accessToken) {
        validateNonBlank(accessToken);
        this.accessToken = accessToken;
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

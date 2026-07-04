package roomescape.auth;

public record AuthToken(Long memberId, String sessionId) {
}

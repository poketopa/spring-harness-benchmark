package roomescape.auth;

public record AuthTokenClaims(Long memberId, String sessionId) {
}

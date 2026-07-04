package roomescape.auth;

public record AuthTokenPayload(Long memberId, String sessionId) {
}

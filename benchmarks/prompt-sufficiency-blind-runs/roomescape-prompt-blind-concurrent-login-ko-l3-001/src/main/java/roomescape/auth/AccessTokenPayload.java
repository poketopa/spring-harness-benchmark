package roomescape.auth;

public record AccessTokenPayload(Long memberId, String loginTokenId) {
}

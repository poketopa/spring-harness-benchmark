package roomescape.auth;

public record IssuedToken(String accessToken, Long memberId, String tokenId) {
}

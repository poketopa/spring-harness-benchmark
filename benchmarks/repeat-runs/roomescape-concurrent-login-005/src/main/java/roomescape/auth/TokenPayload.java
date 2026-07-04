package roomescape.auth;

public record TokenPayload(Long memberId, String tokenId) {
}

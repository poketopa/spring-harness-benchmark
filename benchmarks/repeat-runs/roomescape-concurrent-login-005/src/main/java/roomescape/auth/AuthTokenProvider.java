package roomescape.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Component
public class AuthTokenProvider {

    private static final String PREFIX = "member:";
    private static final String TOKEN_SEPARATOR = ":token:";

    public IssuedToken issueToken(Member member) {
        return issueToken(member.getId());
    }

    public IssuedToken issueToken(Long memberId) {
        String tokenId = UUID.randomUUID().toString();
        String payload = PREFIX + memberId + TOKEN_SEPARATOR + tokenId;
        String accessToken = encode(payload);
        return new IssuedToken(accessToken, memberId, tokenId);
    }

    private String encode(String payload) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public TokenPayload extractPayload(String token) {
        String payload = decode(token);
        if (!payload.startsWith(PREFIX) || !payload.contains(TOKEN_SEPARATOR)) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }

        String[] values = payload.substring(PREFIX.length()).split(TOKEN_SEPARATOR, 2);
        try {
            return new TokenPayload(Long.parseLong(values[0]), values[1]);
        } catch (RuntimeException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }

    public Long extractMemberId(String token) {
        return extractPayload(token).memberId();
    }

    private String decode(String token) {
        try {
            return new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }
}

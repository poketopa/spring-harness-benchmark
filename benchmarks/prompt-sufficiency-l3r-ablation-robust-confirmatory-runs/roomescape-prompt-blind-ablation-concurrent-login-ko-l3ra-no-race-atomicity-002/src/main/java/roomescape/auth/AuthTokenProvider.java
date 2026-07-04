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
    private static final String DELIMITER = ":";

    public String newSessionId() {
        return UUID.randomUUID().toString();
    }

    public String createToken(Long memberId, String sessionId) {
        String payload = PREFIX + memberId + DELIMITER + sessionId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Member member) {
        return createToken(member.getId(), newSessionId());
    }

    public AuthTokenPayload extractPayload(String token) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            if (!payload.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Invalid token prefix");
            }
            String[] values = payload.substring(PREFIX.length()).split(DELIMITER, 2);
            if (values.length != 2 || values[1].isBlank()) {
                throw new IllegalArgumentException("Invalid token payload");
            }
            return new AuthTokenPayload(Long.parseLong(values[0]), values[1]);
        } catch (RuntimeException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }

    public Long extractMemberId(String token) {
        return extractPayload(token).memberId();
    }
}

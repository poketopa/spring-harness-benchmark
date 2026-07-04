package roomescape.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Component;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Component
public class AuthTokenProvider {

    private static final String PREFIX = "member:";
    private static final String DELIMITER = ":";

    public String createSessionKey() {
        return UUID.randomUUID().toString();
    }

    public String createToken(Long memberId, String sessionKey) {
        String payload = PREFIX + memberId + DELIMITER + sessionKey + DELIMITER + UUID.randomUUID();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public TokenPayload extractPayload(String token) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            if (!payload.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Invalid token prefix");
            }
            String[] values = payload.substring(PREFIX.length()).split(DELIMITER);
            if (values.length != 3) {
                throw new IllegalArgumentException("Invalid token values");
            }
            return new TokenPayload(Long.parseLong(values[0]), values[1]);
        } catch (RuntimeException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }
}

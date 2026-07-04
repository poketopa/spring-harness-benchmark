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
    private static final String SESSION_SEPARATOR = ":session:";

    public String createToken(Member member, String sessionId) {
        String payload = PREFIX + member.getId() + SESSION_SEPARATOR + sessionId;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public String createSessionId() {
        return UUID.randomUUID().toString();
    }

    public AuthTokenPayload extractPayload(String token) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            if (!payload.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Invalid token prefix");
            }
            String tokenBody = payload.substring(PREFIX.length());
            int separatorIndex = tokenBody.indexOf(SESSION_SEPARATOR);
            if (separatorIndex < 1) {
                throw new IllegalArgumentException("Invalid token body");
            }
            Long memberId = Long.parseLong(tokenBody.substring(0, separatorIndex));
            String sessionId = tokenBody.substring(separatorIndex + SESSION_SEPARATOR.length());
            if (sessionId.isBlank()) {
                throw new IllegalArgumentException("Invalid session id");
            }
            return new AuthTokenPayload(memberId, sessionId);
        } catch (RuntimeException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }
}

package roomescape.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Component
public class AuthTokenProvider {

    private static final String PREFIX = "member:";
    private static final String SESSION_MARKER = ":session:";

    public String createToken(Member member, String sessionKey) {
        String payload = PREFIX + member.getId() + SESSION_MARKER + sessionKey;
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
            int sessionMarkerIndex = payload.indexOf(SESSION_MARKER, PREFIX.length());
            if (sessionMarkerIndex < 0) {
                throw new IllegalArgumentException("Invalid token session marker");
            }

            Long memberId = Long.parseLong(payload.substring(PREFIX.length(), sessionMarkerIndex));
            String sessionKey = payload.substring(sessionMarkerIndex + SESSION_MARKER.length());
            if (sessionKey.isBlank()) {
                throw new IllegalArgumentException("Invalid token session key");
            }
            return new TokenPayload(memberId, sessionKey);
        } catch (RuntimeException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }

    public record TokenPayload(Long memberId, String sessionKey) {
    }
}

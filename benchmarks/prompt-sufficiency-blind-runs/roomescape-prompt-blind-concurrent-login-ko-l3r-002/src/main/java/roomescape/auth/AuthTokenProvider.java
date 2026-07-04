package roomescape.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.springframework.stereotype.Component;
import roomescape.domain.Member;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

@Component
public class AuthTokenProvider {

    private static final String MEMBER_PREFIX = "member:";
    private static final String SESSION_DELIMITER = ":session:";

    public String createToken(Member member, String sessionKey) {
        String payload = MEMBER_PREFIX + member.getId() + SESSION_DELIMITER + sessionKey;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public AuthTokenPayload extractPayload(String token) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            if (!payload.startsWith(MEMBER_PREFIX)) {
                throw new IllegalArgumentException("Invalid token prefix");
            }
            int sessionDelimiterIndex = payload.indexOf(SESSION_DELIMITER, MEMBER_PREFIX.length());
            if (sessionDelimiterIndex < 0) {
                throw new IllegalArgumentException("Missing session delimiter");
            }

            Long memberId = Long.parseLong(payload.substring(MEMBER_PREFIX.length(), sessionDelimiterIndex));
            String sessionKey = payload.substring(sessionDelimiterIndex + SESSION_DELIMITER.length());
            if (sessionKey.isBlank()) {
                throw new IllegalArgumentException("Missing session key");
            }
            return new AuthTokenPayload(memberId, sessionKey);
        } catch (RuntimeException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }

    public Long extractMemberId(String token) {
        return extractPayload(token).memberId();
    }
}

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
    private static final String DELIMITER = ":";

    public String createToken(Member member, long loginVersion) {
        String payload = PREFIX + member.getId() + DELIMITER + loginVersion;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public AuthToken extractToken(String token) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            if (!payload.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Invalid token prefix");
            }
            String[] values = payload.substring(PREFIX.length()).split(DELIMITER);
            if (values.length != 2) {
                throw new IllegalArgumentException("Invalid token format");
            }
            return new AuthToken(Long.parseLong(values[0]), Long.parseLong(values[1]));
        } catch (RuntimeException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }

    public record AuthToken(Long memberId, long loginVersion) {
    }
}

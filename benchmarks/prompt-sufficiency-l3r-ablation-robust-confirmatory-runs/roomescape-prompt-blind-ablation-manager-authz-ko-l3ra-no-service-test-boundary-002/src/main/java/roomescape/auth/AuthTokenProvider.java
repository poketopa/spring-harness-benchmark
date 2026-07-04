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

    public String createToken(Member member) {
        String payload = PREFIX + member.getId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }

    public Long extractMemberId(String token) {
        try {
            String payload = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            if (!payload.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Invalid token prefix");
            }
            return Long.parseLong(payload.substring(PREFIX.length()));
        } catch (RuntimeException exception) {
            throw new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다.");
        }
    }
}

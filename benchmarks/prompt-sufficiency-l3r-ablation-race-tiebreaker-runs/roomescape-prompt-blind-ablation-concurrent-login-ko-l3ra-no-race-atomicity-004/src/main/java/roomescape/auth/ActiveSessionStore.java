package roomescape.auth;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class ActiveSessionStore {

    private final ConcurrentMap<Long, String> activeSessionIds = new ConcurrentHashMap<>();

    public String activate(Long memberId) {
        String sessionId = UUID.randomUUID().toString();
        activeSessionIds.put(memberId, sessionId);
        return sessionId;
    }

    public boolean isActive(AuthTokenClaims claims) {
        return claims.sessionId().equals(activeSessionIds.get(claims.memberId()));
    }
}

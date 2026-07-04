package roomescape.auth;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class ActiveLoginSessionStore {

    private final ConcurrentMap<Long, String> activeTokens = new ConcurrentHashMap<>();

    public void renew(Long memberId, String accessToken) {
        activeTokens.put(memberId, accessToken);
    }

    public boolean isCurrent(Long memberId, String accessToken) {
        return accessToken.equals(activeTokens.get(memberId));
    }
}

package roomescape.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.ActiveSession;

public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {

    Optional<ActiveSession> findByMemberId(Long memberId);

    boolean existsByMemberIdAndSessionKey(Long memberId, String sessionKey);
}

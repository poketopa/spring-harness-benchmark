package roomescape.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.ActiveSession;
import roomescape.domain.Member;

public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {

    Optional<ActiveSession> findByMember(Member member);

    boolean existsByMemberIdAndAccessToken(Long memberId, String accessToken);
}

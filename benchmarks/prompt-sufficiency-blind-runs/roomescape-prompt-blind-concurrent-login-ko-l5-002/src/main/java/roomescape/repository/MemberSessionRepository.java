package roomescape.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.MemberSession;

public interface MemberSessionRepository extends JpaRepository<MemberSession, Long> {

    Optional<MemberSession> findByMemberId(Long memberId);

    boolean existsByMemberIdAndSessionKey(Long memberId, String sessionKey);
}

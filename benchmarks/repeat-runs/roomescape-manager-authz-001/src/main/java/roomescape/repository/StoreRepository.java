package roomescape.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Member;
import roomescape.domain.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {

    boolean existsByManager(Member manager);

    Optional<Store> findByManager(Member manager);
}

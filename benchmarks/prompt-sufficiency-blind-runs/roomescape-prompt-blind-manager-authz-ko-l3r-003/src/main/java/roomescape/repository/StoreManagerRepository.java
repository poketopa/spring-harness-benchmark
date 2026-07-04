package roomescape.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Member;
import roomescape.domain.Store;
import roomescape.domain.StoreManager;

public interface StoreManagerRepository extends JpaRepository<StoreManager, Long> {

    boolean existsByManagerAndStore(Member manager, Store store);
}

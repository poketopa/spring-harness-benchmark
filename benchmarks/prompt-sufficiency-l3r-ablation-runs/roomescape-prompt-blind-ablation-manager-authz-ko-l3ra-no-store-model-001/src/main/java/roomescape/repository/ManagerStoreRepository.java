package roomescape.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.ManagerStore;
import roomescape.domain.Member;
import roomescape.domain.Store;

public interface ManagerStoreRepository extends JpaRepository<ManagerStore, Long> {

    boolean existsByManagerAndStore(Member manager, Store store);
}

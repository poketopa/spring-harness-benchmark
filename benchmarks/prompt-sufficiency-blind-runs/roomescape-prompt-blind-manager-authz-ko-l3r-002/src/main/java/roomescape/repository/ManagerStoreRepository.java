package roomescape.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import roomescape.domain.ManagerStore;
import roomescape.domain.Member;
import roomescape.domain.Store;

public interface ManagerStoreRepository extends JpaRepository<ManagerStore, Long> {

    boolean existsByManagerAndStore(Member manager, Store store);

    @Query("""
            select managerStore.store
            from ManagerStore managerStore
            where managerStore.manager = :manager
            """)
    List<Store> findStoresByManager(@Param("manager") Member manager);
}

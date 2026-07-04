package roomescape.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Store;

public interface StoreRepository extends JpaRepository<Store, Long> {
}

package roomescape.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Member;
import roomescape.domain.Store;
import roomescape.domain.Theme;

public interface StoreRepository extends JpaRepository<Store, Long> {

    boolean existsByManagerAndTheme(Member manager, Theme theme);

    List<Store> findAllByManager(Member manager);
}

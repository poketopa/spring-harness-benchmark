package roomescape.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.ManagedStore;
import roomescape.domain.Member;
import roomescape.domain.Theme;

public interface ManagedStoreRepository extends JpaRepository<ManagedStore, Long> {

    @EntityGraph(attributePaths = {"theme"})
    List<ManagedStore> findAllByManager(Member manager);

    boolean existsByManagerAndTheme(Member manager, Theme theme);
}

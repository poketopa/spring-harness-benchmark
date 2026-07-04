package roomescape.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Store;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

public interface StoreRepository extends JpaRepository<Store, Long> {

    String DEFAULT_STORE_NAME = "기본 매장";

    Optional<Store> findByName(String name);

    default Store getDefaultStore() {
        return findByName(DEFAULT_STORE_NAME)
                .orElseGet(() -> save(new Store(DEFAULT_STORE_NAME)));
    }

    default Store getByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.STORE_NOT_FOUND, "매장을 찾을 수 없습니다."));
    }
}

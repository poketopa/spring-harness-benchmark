package roomescape.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Store;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

public interface StoreRepository extends JpaRepository<Store, Long> {

    default Store getByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.INVALID_INPUT, "매장을 찾을 수 없습니다."));
    }
}

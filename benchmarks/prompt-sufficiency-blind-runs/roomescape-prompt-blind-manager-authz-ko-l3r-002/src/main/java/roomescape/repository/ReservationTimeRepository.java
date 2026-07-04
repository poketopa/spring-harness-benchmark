package roomescape.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.ReservationTime;
import roomescape.domain.Store;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;

public interface ReservationTimeRepository extends JpaRepository<ReservationTime, Long> {

    List<ReservationTime> findAllByStore(Store store);

    default ReservationTime getByIdOrThrow(Long id) {
        return findById(id)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_TIME_NOT_FOUND, "예약 시간을 찾을 수 없습니다."));
    }
}

package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.Store;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.StoreManagerRepository;

@Service
@Transactional(readOnly = true)
public class ManagerAuthorizationService {

    private final StoreManagerRepository storeManagerRepository;

    public ManagerAuthorizationService(StoreManagerRepository storeManagerRepository) {
        this.storeManagerRepository = storeManagerRepository;
    }

    public void validateCanManageReservation(Member member, Reservation reservation) {
        validateCanManageStore(member, reservation.getTheme().getStore());
    }

    public void validateCanManageStore(Member member, Store store) {
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매장 매니저 권한이 필요합니다.");
        }
        if (store == null || !storeManagerRepository.existsByManagerAndStore(member, store)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리하는 매장의 예약만 접근할 수 있습니다.");
        }
    }
}

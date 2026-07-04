package roomescape.service;

import org.springframework.stereotype.Service;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.Role;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.StoreRepository;

@Service
public class ManagerAuthorizationService {

    private final StoreRepository storeRepository;

    public ManagerAuthorizationService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public void validateManager(Member member) {
        if (member.getRole() != Role.MANAGER) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매장 매니저 권한이 필요합니다.");
        }
    }

    public void validateManagesReservation(Member manager, Reservation reservation) {
        validateManager(manager);
        if (!storeRepository.existsByManagerAndTheme(manager, reservation.getTheme())) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리하는 매장의 예약만 접근할 수 있습니다.");
        }
    }
}

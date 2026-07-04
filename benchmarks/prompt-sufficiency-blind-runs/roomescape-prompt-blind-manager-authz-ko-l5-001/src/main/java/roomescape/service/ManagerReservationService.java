package roomescape.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Store;
import roomescape.dto.ReservationChangeRequest;
import roomescape.dto.ReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.StoreRepository;

@Service
@Transactional(readOnly = true)
public class ManagerReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final StoreRepository storeRepository;
    private final AuthenticatedMemberService authenticatedMemberService;
    private final ReservationService reservationService;

    public ManagerReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeRepository reservationTimeRepository,
            StoreRepository storeRepository,
            AuthenticatedMemberService authenticatedMemberService,
            ReservationService reservationService
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.storeRepository = storeRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.reservationService = reservationService;
    }

    public List<ReservationResponse> findByStore(LoginMember loginMember, Long storeId) {
        Store store = storeRepository.getByIdOrThrow(storeId);
        authorizeManager(loginMember, store);

        return reservationRepository.findAllByThemeStoreOrderByDateAscTimeStartAtAsc(store)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse change(
            LoginMember loginMember,
            Long storeId,
            Long reservationId,
            ReservationChangeRequest request
    ) {
        Store store = storeRepository.getByIdOrThrow(storeId);
        authorizeManager(loginMember, store);
        Reservation reservation = findManagedReservation(reservationId, store);
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());

        return reservationService.changeManagedReservation(reservation, time, request);
    }

    @Transactional
    public void cancel(LoginMember loginMember, Long storeId, Long reservationId) {
        Store store = storeRepository.getByIdOrThrow(storeId);
        authorizeManager(loginMember, store);
        Reservation reservation = findManagedReservation(reservationId, store);

        reservationService.cancelManagedReservation(reservation);
    }

    private void authorizeManager(LoginMember loginMember, Store store) {
        Member member = authenticatedMemberService.findMember(loginMember);
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매니저 권한이 필요합니다.");
        }
        if (!member.manages(store)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리 매장의 예약만 접근할 수 있습니다.");
        }
    }

    private Reservation findManagedReservation(Long reservationId, Store store) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_NOT_FOUND, "예약을 찾을 수 없습니다."));
        if (!reservation.belongsTo(store)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리 매장의 예약만 접근할 수 있습니다.");
        }
        return reservation;
    }
}

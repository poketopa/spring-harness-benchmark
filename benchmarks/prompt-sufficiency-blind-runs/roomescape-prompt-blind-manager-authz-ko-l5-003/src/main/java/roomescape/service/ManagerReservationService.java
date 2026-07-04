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

    public List<ReservationResponse> findAll(LoginMember loginMember, Long storeId) {
        Member manager = authenticatedMemberService.findMember(loginMember);
        Store store = resolveAuthorizedStore(manager, storeId);
        return reservationRepository.findAllByThemeStoreOrderByDateAscTimeStartAtAsc(store)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    private Store resolveAuthorizedStore(Member manager, Long storeId) {
        validateManager(manager);
        Store store = storeRepository.getByIdOrThrow(storeId);
        validateStoreManager(store, manager);
        return store;
    }

    private void validateManager(Member member) {
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매장 매니저 권한이 필요합니다.");
        }
    }

    @Transactional
    public ReservationResponse change(LoginMember loginMember, Long reservationId, ReservationChangeRequest request) {
        Member manager = authenticatedMemberService.findMember(loginMember);
        Reservation reservation = findManagedReservation(reservationId, manager);
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());
        return reservationService.changeManaged(reservation, time, request);
    }

    @Transactional
    public void cancel(LoginMember loginMember, Long reservationId) {
        Member manager = authenticatedMemberService.findMember(loginMember);
        Reservation reservation = findManagedReservation(reservationId, manager);
        reservationService.cancelManaged(reservation);
    }

    private Reservation findManagedReservation(Long reservationId, Member manager) {
        validateManager(manager);
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_NOT_FOUND, "예약을 찾을 수 없습니다."));
        validateStoreManager(reservation.getTheme().getStore(), manager);
        return reservation;
    }

    private void validateStoreManager(Store store, Member manager) {
        if (!store.isManagedBy(manager)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "담당 매장의 예약만 접근할 수 있습니다.");
        }
    }
}

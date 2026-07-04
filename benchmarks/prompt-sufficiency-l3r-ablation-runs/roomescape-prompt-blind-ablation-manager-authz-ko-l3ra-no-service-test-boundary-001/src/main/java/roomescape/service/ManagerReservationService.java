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

@Service
@Transactional(readOnly = true)
public class ManagerReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final AuthenticatedMemberService authenticatedMemberService;
    private final ReservationService reservationService;

    public ManagerReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeRepository reservationTimeRepository,
            AuthenticatedMemberService authenticatedMemberService,
            ReservationService reservationService
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.reservationService = reservationService;
    }

    public List<ReservationResponse> findManagedReservations(LoginMember loginMember) {
        Member manager = findManager(loginMember);
        return reservationRepository.findAllByThemeStoreManagerOrderByDateAscTimeStartAtAsc(manager)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public ReservationResponse findManagedReservation(LoginMember loginMember, Long reservationId) {
        Member manager = findManager(loginMember);
        Reservation reservation = findManagedReservation(reservationId, manager);
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse change(LoginMember loginMember, Long reservationId, ReservationChangeRequest request) {
        Member manager = findManager(loginMember);
        Reservation reservation = findManagedReservation(reservationId, manager);
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());

        return reservationService.changeManagedSchedule(reservation, time, request);
    }

    @Transactional
    public void cancel(LoginMember loginMember, Long reservationId) {
        Member manager = findManager(loginMember);
        Reservation reservation = findManagedReservation(reservationId, manager);

        reservationService.cancelManagedReservation(reservation);
    }

    private Member findManager(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매니저 권한이 필요합니다.");
        }
        return member;
    }

    private Reservation findManagedReservation(Long reservationId, Member manager) {
        Reservation reservation = reservationRepository.findReservationById(reservationId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_NOT_FOUND, "예약을 찾을 수 없습니다."));
        validateManagedStore(reservation, manager);
        return reservation;
    }

    private void validateManagedStore(Reservation reservation, Member manager) {
        Store store = reservation.getTheme().getStore();
        if (store == null || !store.isManagedBy(manager)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리하는 매장의 예약만 접근할 수 있습니다.");
        }
    }
}

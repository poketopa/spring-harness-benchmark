package roomescape.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.dto.ReservationChangeRequest;
import roomescape.dto.ReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ManagerStoreRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.StoreRepository;

@Service
@Transactional(readOnly = true)
public class ManagerReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final StoreRepository storeRepository;
    private final ManagerStoreRepository managerStoreRepository;
    private final AuthenticatedMemberService authenticatedMemberService;
    private final WaitingPromotionService waitingPromotionService;
    private final Clock clock;

    public ManagerReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeRepository reservationTimeRepository,
            StoreRepository storeRepository,
            ManagerStoreRepository managerStoreRepository,
            AuthenticatedMemberService authenticatedMemberService,
            WaitingPromotionService waitingPromotionService,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.storeRepository = storeRepository;
        this.managerStoreRepository = managerStoreRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.waitingPromotionService = waitingPromotionService;
        this.clock = clock;
    }

    public List<ReservationResponse> findStoreReservations(LoginMember loginMember, Long storeId) {
        Member manager = authenticatedManager(loginMember);
        Store store = storeRepository.getByIdOrThrow(storeId);
        validateManagedStore(manager, store);

        return reservationRepository.findAllByThemeStoreOrderByDateAscTimeStartAtAsc(store)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse change(LoginMember loginMember, Long reservationId, ReservationChangeRequest request) {
        Member manager = authenticatedManager(loginMember);
        Reservation reservation = managedReservation(reservationId, manager);
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());

        return changeSchedule(reservation, time, request);
    }

    @Transactional
    public void cancel(LoginMember loginMember, Long reservationId) {
        Member manager = authenticatedManager(loginMember);
        Reservation reservation = managedReservation(reservationId, manager);

        cancelReservation(reservation);
    }

    private Member authenticatedManager(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매니저 권한이 필요합니다.");
        }
        return member;
    }

    private Reservation managedReservation(Long reservationId, Member manager) {
        Reservation reservation = reservationRepository.findWithManagerViewById(reservationId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_NOT_FOUND, "예약을 찾을 수 없습니다."));
        validateManagedStore(manager, reservation.getTheme().getStore());
        return reservation;
    }

    private void validateManagedStore(Member manager, Store store) {
        if (store == null || !managerStoreRepository.existsByManagerAndStore(manager, store)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리하는 매장의 예약만 접근할 수 있습니다.");
        }
    }

    private ReservationResponse changeSchedule(
            Reservation reservation,
            ReservationTime time,
            ReservationChangeRequest request
    ) {
        LocalDateTime now = LocalDateTime.now(clock);
        Theme previousTheme = reservation.getTheme();
        ReservationTime previousTime = reservation.getTime();
        LocalDate previousDate = reservation.getDate();
        boolean scheduleChanged = !reservation.hasSchedule(request.date(), time);

        validateChangeAllowed(reservation, time, request, now);
        reservation.changeSchedule(request.date(), time);

        ReservationResponse response = saveReservation(reservation);
        if (scheduleChanged) {
            waitingPromotionService.promoteFirstWaiting(previousTheme, previousTime, previousDate);
        }
        return response;
    }

    private void validateChangeAllowed(
            Reservation reservation,
            ReservationTime time,
            ReservationChangeRequest request,
            LocalDateTime now
    ) {
        if (reservation.isPast(now)) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 예약은 변경할 수 없습니다.");
        }
        if (reservation.isPastSchedule(request.date(), time, now)) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간으로 변경할 수 없습니다.");
        }
        if (reservationRepository.existsByThemeAndTimeAndDateAndIdNot(
                reservation.getTheme(),
                time,
                request.date(),
                reservation.getId()
        )) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }

    private ReservationResponse saveReservation(Reservation reservation) {
        try {
            return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }

    private void cancelReservation(Reservation reservation) {
        Theme theme = reservation.getTheme();
        ReservationTime time = reservation.getTime();
        LocalDate date = reservation.getDate();

        if (reservation.isPast(LocalDateTime.now(clock))) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 예약은 취소할 수 없습니다.");
        }

        reservationRepository.delete(reservation);
        reservationRepository.flush();
        waitingPromotionService.promoteFirstWaiting(theme, time, date);
    }
}

package roomescape.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
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
    private final ManagerReservationAuthorizationService authorizationService;
    private final WaitingPromotionService waitingPromotionService;
    private final Clock clock;

    public ManagerReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeRepository reservationTimeRepository,
            StoreRepository storeRepository,
            ManagerReservationAuthorizationService authorizationService,
            WaitingPromotionService waitingPromotionService,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.storeRepository = storeRepository;
        this.authorizationService = authorizationService;
        this.waitingPromotionService = waitingPromotionService;
        this.clock = clock;
    }

    public List<ReservationResponse> findByStore(LoginMember loginMember, Long storeId) {
        Store store = storeRepository.getByIdOrThrow(storeId);
        authorizationService.authorizeStoreAccess(loginMember, store);
        return reservationRepository.findAllByThemeStoreOrderByDateAscTimeStartAtAsc(store)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    public ReservationResponse find(LoginMember loginMember, Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        authorizationService.authorizeStoreAccess(loginMember, reservation.getTheme().getStore());
        return ReservationResponse.from(reservation);
    }

    @Transactional
    public ReservationResponse change(LoginMember loginMember, Long reservationId, ReservationChangeRequest request) {
        Reservation reservation = findReservation(reservationId);
        Store store = reservation.getTheme().getStore();
        authorizationService.authorizeStoreAccess(loginMember, store);

        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());
        validateTimeBelongsToStore(time, store);
        return changeSchedule(reservation, time, request);
    }

    private ReservationResponse changeSchedule(
            Reservation reservation,
            ReservationTime time,
            ReservationChangeRequest request
    ) {
        LocalDateTime now = LocalDateTime.now(clock);
        ReservationTime previousTime = reservation.getTime();
        LocalDate previousDate = reservation.getDate();
        boolean scheduleChanged = !reservation.hasSchedule(request.date(), time);

        validateChangeAllowed(reservation, time, request, now);
        reservation.changeSchedule(request.date(), time);

        ReservationResponse response = saveReservation(reservation);
        if (scheduleChanged) {
            waitingPromotionService.promoteFirstWaiting(reservation.getTheme(), previousTime, previousDate);
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

    @Transactional
    public void cancel(LoginMember loginMember, Long reservationId) {
        Reservation reservation = findReservation(reservationId);
        authorizationService.authorizeStoreAccess(loginMember, reservation.getTheme().getStore());

        ReservationTime time = reservation.getTime();
        LocalDate date = reservation.getDate();
        if (reservation.isPast(LocalDateTime.now(clock))) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 예약은 취소할 수 없습니다.");
        }

        reservationRepository.delete(reservation);
        reservationRepository.flush();
        waitingPromotionService.promoteFirstWaiting(reservation.getTheme(), time, date);
    }

    private Reservation findReservation(Long reservationId) {
        return reservationRepository.findByIdWithDetails(reservationId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_NOT_FOUND, "예약을 찾을 수 없습니다."));
    }

    private void validateTimeBelongsToStore(ReservationTime time, Store store) {
        if (!time.belongsTo(store)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리 중인 매장의 예약 시간만 사용할 수 있습니다.");
        }
    }

    private ReservationResponse saveReservation(Reservation reservation) {
        try {
            return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }
}

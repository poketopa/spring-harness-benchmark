package roomescape.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private final ManagerAuthorizationService managerAuthorizationService;
    private final WaitingPromotionService waitingPromotionService;
    private final Clock clock;

    public ManagerReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeRepository reservationTimeRepository,
            StoreRepository storeRepository,
            AuthenticatedMemberService authenticatedMemberService,
            ManagerAuthorizationService managerAuthorizationService,
            WaitingPromotionService waitingPromotionService,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.storeRepository = storeRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.managerAuthorizationService = managerAuthorizationService;
        this.waitingPromotionService = waitingPromotionService;
        this.clock = clock;
    }

    public List<ReservationResponse> findManagedReservations(LoginMember loginMember) {
        Member manager = authenticatedMemberService.findMember(loginMember);
        managerAuthorizationService.validateManager(manager);

        List<Theme> managedThemes = storeRepository.findAllByManager(manager)
                .stream()
                .map(Store::getTheme)
                .toList();
        if (managedThemes.isEmpty()) {
            return List.of();
        }
        return reservationRepository.findAllByThemeInOrderByDateAscTimeStartAtAsc(managedThemes)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse change(LoginMember loginMember, Long reservationId, ReservationChangeRequest request) {
        Member manager = authenticatedMemberService.findMember(loginMember);
        Reservation reservation = findReservation(reservationId);
        managerAuthorizationService.validateManagesReservation(manager, reservation);
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());

        return changeSchedule(reservation, time, request);
    }

    private Reservation findReservation(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_NOT_FOUND, "예약을 찾을 수 없습니다."));
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

        ReservationResponse response = ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
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

    @Transactional
    public void cancel(LoginMember loginMember, Long reservationId) {
        Member manager = authenticatedMemberService.findMember(loginMember);
        Reservation reservation = findReservation(reservationId);
        managerAuthorizationService.validateManagesReservation(manager, reservation);

        cancelReservation(reservation);
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

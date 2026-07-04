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
import roomescape.domain.Theme;
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
    private final WaitingPromotionService waitingPromotionService;
    private final Clock clock;

    public ManagerReservationService(
            ReservationRepository reservationRepository,
            ReservationTimeRepository reservationTimeRepository,
            AuthenticatedMemberService authenticatedMemberService,
            WaitingPromotionService waitingPromotionService,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.waitingPromotionService = waitingPromotionService;
        this.clock = clock;
    }

    public List<ReservationResponse> findManagedReservations(LoginMember loginMember) {
        Member manager = findManager(loginMember);
        return reservationRepository.findAllByThemeStoreManagerOrderByDateAscTimeStartAtAsc(manager)
                .stream()
                .map(ReservationResponse::from)
                .toList();
    }

    @Transactional
    public ReservationResponse change(LoginMember loginMember, Long reservationId, ReservationChangeRequest request) {
        Member manager = findManager(loginMember);
        Reservation reservation = findManagedReservation(reservationId, manager);
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());

        return changeSchedule(reservation, time, request);
    }

    @Transactional
    public void cancel(LoginMember loginMember, Long reservationId) {
        Member manager = findManager(loginMember);
        Reservation reservation = findManagedReservation(reservationId, manager);

        cancelReservation(reservation);
    }

    private Member findManager(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);
        if (!member.isManager()) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "매니저 권한이 필요합니다.");
        }
        return member;
    }

    private Reservation findManagedReservation(Long reservationId, Member manager) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_NOT_FOUND, "예약을 찾을 수 없습니다."));
        if (!reservation.isManagedBy(manager)) {
            throw new RoomescapeException(ErrorCode.FORBIDDEN, "관리하는 매장의 예약만 접근할 수 있습니다.");
        }
        return reservation;
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

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
import roomescape.domain.Role;
import roomescape.domain.Theme;
import roomescape.dto.ReservationChangeRequest;
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ThemeRepository themeRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final AuthenticatedMemberService authenticatedMemberService;
    private final WaitingPromotionService waitingPromotionService;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            AuthenticatedMemberService authenticatedMemberService,
            WaitingPromotionService waitingPromotionService,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.themeRepository = themeRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.waitingPromotionService = waitingPromotionService;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(LoginMember loginMember, ReservationRequest request) {
        Member member = authenticatedMemberService.findMember(loginMember);
        Theme theme = themeRepository.getByIdOrThrow(request.themeId());
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());
        Reservation reservation = new Reservation(member, theme, time, request.date());

        validateCreateAllowed(reservation, theme, time, request.date(), LocalDateTime.now(clock));
        return saveReservation(reservation);
    }

    private void validateCreateAllowed(
            Reservation reservation,
            Theme theme,
            ReservationTime time,
            LocalDate date,
            LocalDateTime now
    ) {
        if (reservation.isPast(now)) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간은 예약할 수 없습니다.");
        }
        if (reservationRepository.existsByThemeAndTimeAndDate(theme, time, date)) {
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

    public List<ReservationResponse> findManageable(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);

        if (member.getRole() == Role.ADMIN) {
            return reservationRepository.findAllByOrderByDateAscTimeStartAtAsc()
                    .stream()
                    .map(ReservationResponse::from)
                    .toList();
        }
        if (member.getRole() == Role.MANAGER) {
            return reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                    .stream()
                    .map(ReservationResponse::from)
                    .toList();
        }
        throw new RoomescapeException(ErrorCode.FORBIDDEN, "예약 관리 권한이 없습니다.");
    }

    @Transactional
    public ReservationResponse change(LoginMember loginMember, Long reservationId, ReservationChangeRequest request) {
        Member member = authenticatedMemberService.findMember(loginMember);
        Reservation reservation = findOwnReservation(reservationId, member);
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());

        return changeSchedule(reservation, time, request);
    }

    private Reservation findOwnReservation(Long reservationId, Member member) {
        return reservationRepository.findByIdAndMember(reservationId, member)
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

    @Transactional
    public void cancel(LoginMember loginMember, Long reservationId) {
        Member member = authenticatedMemberService.findMember(loginMember);
        Reservation reservation = findOwnReservation(reservationId, member);

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

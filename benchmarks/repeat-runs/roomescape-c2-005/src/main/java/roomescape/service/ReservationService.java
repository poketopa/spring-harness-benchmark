package roomescape.service;

import java.time.Clock;
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
import roomescape.dto.MyReservationResponse;
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
    private final AuthenticatedMemberService authenticatedMemberService;
    private final ThemeRepository themeRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final WaitingPromotionService waitingPromotionService;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            AuthenticatedMemberService authenticatedMemberService,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            WaitingPromotionService waitingPromotionService,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.themeRepository = themeRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.waitingPromotionService = waitingPromotionService;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(LoginMember loginMember, ReservationRequest request) {
        Member member = authenticatedMemberService.getMember(loginMember);
        Theme theme = findTheme(request.themeId());
        ReservationTime time = findTime(request.timeId());
        Reservation reservation = new Reservation(member, theme, time, request.date());

        validateCreateAllowed(reservation);

        try {
            return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }

    private void validateCreateAllowed(Reservation reservation) {
        if (reservation.isPast(LocalDateTime.now(clock))) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간은 예약할 수 없습니다.");
        }
        if (reservationRepository.existsByThemeAndTimeAndDate(
                reservation.getTheme(),
                reservation.getTime(),
                reservation.getDate()
        )) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }

    @Transactional
    public ReservationResponse change(LoginMember loginMember, Long reservationId, ReservationChangeRequest request) {
        Member member = authenticatedMemberService.getMember(loginMember);
        Reservation reservation = findOwnedReservation(reservationId, member);
        ReservationTime targetTime = findTime(request.timeId());

        validateChangeAllowed(reservation, targetTime, request);

        reservation.changeSchedule(targetTime, request.date());
        try {
            return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }

    private Reservation findOwnedReservation(Long reservationId, Member member) {
        return reservationRepository.findByIdAndMember(reservationId, member)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_NOT_FOUND, "예약을 찾을 수 없습니다."));
    }

    private void validateChangeAllowed(
            Reservation reservation,
            ReservationTime targetTime,
            ReservationChangeRequest request
    ) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (reservation.isPast(now)) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 예약은 변경할 수 없습니다.");
        }
        if (reservation.isPastSchedule(targetTime, request.date(), now)) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간으로 변경할 수 없습니다.");
        }
        if (!reservation.isSameSlot(reservation.getTheme(), targetTime, request.date())
                && reservationRepository.existsByThemeAndTimeAndDateAndIdNot(
                        reservation.getTheme(),
                        targetTime,
                        request.date(),
                        reservation.getId()
                )) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }

    private Theme findTheme(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.THEME_NOT_FOUND, "테마를 찾을 수 없습니다."));
    }

    private ReservationTime findTime(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_TIME_NOT_FOUND, "예약 시간을 찾을 수 없습니다."));
    }

    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        Member member = authenticatedMemberService.getMember(loginMember);

        return reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(MyReservationResponse::fromReservation)
                .toList();
    }

    @Transactional
    public void cancel(LoginMember loginMember, Long reservationId) {
        Member member = authenticatedMemberService.getMember(loginMember);
        Reservation reservation = findOwnedReservation(reservationId, member);

        validateCancelAllowed(reservation);

        waitingPromotionService.cancelAndPromote(reservation);
    }

    private void validateCancelAllowed(Reservation reservation) {
        if (reservation.isPast(LocalDateTime.now(clock))) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 예약은 취소할 수 없습니다.");
        }
    }
}

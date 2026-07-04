package roomescape.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;
import roomescape.dto.ReservationRequest;
import roomescape.dto.WaitingResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class WaitingService {

    private final WaitingRepository waitingRepository;
    private final ReservationRepository reservationRepository;
    private final ThemeRepository themeRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final AuthenticatedMemberService authenticatedMemberService;
    private final Clock clock;

    public WaitingService(
            WaitingRepository waitingRepository,
            ReservationRepository reservationRepository,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            AuthenticatedMemberService authenticatedMemberService,
            Clock clock
    ) {
        this.waitingRepository = waitingRepository;
        this.reservationRepository = reservationRepository;
        this.themeRepository = themeRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.clock = clock;
    }

    @Transactional
    public WaitingResponse create(LoginMember loginMember, ReservationRequest request) {
        Member member = authenticatedMemberService.findMember(loginMember);
        Theme theme = findTheme(request.themeId());
        ReservationTime time = findTime(request.timeId());
        Reservation reservation = findReservation(theme, time, request);

        validateCreateAllowed(member, reservation, theme, time, request);

        Waiting waiting = new Waiting(member, theme, time, request.date(), LocalDateTime.now(clock));
        Waiting savedWaiting = saveWaiting(waiting);
        return WaitingResponse.of(savedWaiting, rankOf(savedWaiting));
    }

    private Theme findTheme(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.THEME_NOT_FOUND, "테마를 찾을 수 없습니다."));
    }

    private ReservationTime findTime(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_TIME_NOT_FOUND, "예약 시간을 찾을 수 없습니다."));
    }

    private Reservation findReservation(Theme theme, ReservationTime time, ReservationRequest request) {
        return reservationRepository.findByThemeAndTimeAndDate(theme, time, request.date())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.WAITING_NOT_ALLOWED, "예약된 시간에만 대기할 수 있습니다."));
    }

    private void validateCreateAllowed(
            Member member,
            Reservation reservation,
            Theme theme,
            ReservationTime time,
            ReservationRequest request
    ) {
        if (reservation.isOwnedBy(member)) {
            throw new RoomescapeException(ErrorCode.WAITING_NOT_ALLOWED, "본인의 예약에는 대기할 수 없습니다.");
        }
        if (waitingRepository.existsByMemberAndThemeAndTimeAndDate(member, theme, time, request.date())) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 신청한 시간입니다.");
        }
    }

    private Waiting saveWaiting(Waiting waiting) {
        try {
            return waitingRepository.saveAndFlush(waiting);
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 신청한 시간입니다.");
        }
    }

    public int rankOf(Waiting waiting) {
        return waitingRepository.countRank(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate(),
                waiting.getCreatedAt(),
                waiting.getId()
        );
    }
}

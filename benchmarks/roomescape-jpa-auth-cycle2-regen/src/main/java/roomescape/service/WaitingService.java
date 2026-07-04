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
import roomescape.domain.Waiting;
import roomescape.dto.WaitingRequest;
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
    private final WaitingRankService waitingRankService;
    private final AdminAuthorizationService adminAuthorizationService;
    private final Clock clock;

    public WaitingService(
            WaitingRepository waitingRepository,
            ReservationRepository reservationRepository,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            AuthenticatedMemberService authenticatedMemberService,
            WaitingRankService waitingRankService,
            AdminAuthorizationService adminAuthorizationService,
            Clock clock
    ) {
        this.waitingRepository = waitingRepository;
        this.reservationRepository = reservationRepository;
        this.themeRepository = themeRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.waitingRankService = waitingRankService;
        this.adminAuthorizationService = adminAuthorizationService;
        this.clock = clock;
    }

    @Transactional
    public WaitingResponse create(LoginMember loginMember, WaitingRequest request) {
        Member member = authenticatedMemberService.findMember(loginMember);
        Theme theme = themeRepository.getByIdOrThrow(request.themeId());
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(request.timeId());
        Reservation reservation = findReservedSlot(theme, time, request);
        LocalDateTime now = LocalDateTime.now(clock);
        Waiting waiting = new Waiting(member, theme, time, request.date(), now);

        validateWaitingAllowed(member, reservation, waiting, now);

        try {
            Waiting savedWaiting = waitingRepository.saveAndFlush(waiting);
            return WaitingResponse.of(savedWaiting, waitingRankService.calculate(savedWaiting));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기한 예약입니다.");
        }
    }

    private Reservation findReservedSlot(Theme theme, ReservationTime time, WaitingRequest request) {
        return reservationRepository.findByThemeAndTimeAndDate(theme, time, request.date())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.WAITING_NOT_ALLOWED, "예약된 슬롯에만 대기할 수 있습니다."));
    }

    private void validateWaitingAllowed(
            Member member,
            Reservation reservation,
            Waiting waiting,
            LocalDateTime now
    ) {
        if (reservation.isOwnedBy(member)) {
            throw new RoomescapeException(ErrorCode.WAITING_NOT_ALLOWED, "본인의 예약에는 대기할 수 없습니다.");
        }
        if (waiting.isPast(now)) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간은 대기할 수 없습니다.");
        }
        if (waitingRepository.existsByMemberAndThemeAndTimeAndDate(
                member,
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate()
        )) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기한 예약입니다.");
        }
    }

    public List<WaitingResponse> findAll(LoginMember loginMember) {
        adminAuthorizationService.validateAdmin(loginMember);
        return waitingRepository.findAllByOrderByDateAscTimeStartAtAscCreatedAtAsc()
                .stream()
                .map(waiting -> WaitingResponse.of(waiting, waitingRankService.calculate(waiting)))
                .toList();
    }

    @Transactional
    public void cancel(LoginMember loginMember, Long waitingId) {
        Member member = authenticatedMemberService.findMember(loginMember);
        Waiting waiting = findOwnWaiting(waitingId, member);

        waitingRepository.delete(waiting);
    }

    private Waiting findOwnWaiting(Long waitingId, Member member) {
        return waitingRepository.findByIdAndMember(waitingId, member)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.WAITING_NOT_FOUND, "대기를 찾을 수 없습니다."));
    }

    @Transactional
    public void cancelByAdmin(LoginMember loginMember, Long waitingId) {
        adminAuthorizationService.validateAdmin(loginMember);
        Waiting waiting = findWaiting(waitingId);

        waitingRepository.delete(waiting);
    }

    private Waiting findWaiting(Long waitingId) {
        return waitingRepository.findById(waitingId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.WAITING_NOT_FOUND, "대기를 찾을 수 없습니다."));
    }
}

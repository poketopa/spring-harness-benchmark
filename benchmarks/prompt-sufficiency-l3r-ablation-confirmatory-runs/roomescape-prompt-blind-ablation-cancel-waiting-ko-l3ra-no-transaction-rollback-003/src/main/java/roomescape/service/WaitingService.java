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
import roomescape.dto.MyReservationResponse;
import roomescape.dto.WaitingRequest;
import roomescape.dto.WaitingResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class WaitingService {

    private final WaitingRepository waitingRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ThemeRepository themeRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final Clock clock;

    public WaitingService(
            WaitingRepository waitingRepository,
            ReservationRepository reservationRepository,
            MemberRepository memberRepository,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            Clock clock
    ) {
        this.waitingRepository = waitingRepository;
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
        this.themeRepository = themeRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.clock = clock;
    }

    @Transactional
    public WaitingResponse create(LoginMember loginMember, WaitingRequest request) {
        Member member = findMember(loginMember);
        Theme theme = findTheme(request.themeId());
        ReservationTime time = findTime(request.timeId());
        Reservation reservation = findReservedSlot(theme, time, request);
        LocalDateTime now = LocalDateTime.now(clock);
        Waiting waiting = new Waiting(member, theme, time, request.date(), now);

        validateWaitingAllowed(member, reservation, waiting, now);

        try {
            Waiting savedWaiting = waitingRepository.saveAndFlush(waiting);
            return WaitingResponse.of(savedWaiting, calculateRank(savedWaiting));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기한 예약입니다.");
        }
    }

    private Member findMember(LoginMember loginMember) {
        return memberRepository.findById(loginMember.id())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
    }

    private Theme findTheme(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.THEME_NOT_FOUND, "테마를 찾을 수 없습니다."));
    }

    private ReservationTime findTime(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_TIME_NOT_FOUND, "예약 시간을 찾을 수 없습니다."));
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

    private int calculateRank(Waiting waiting) {
        long previousWaitingCount = waitingRepository.countEarlierWaitings(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate(),
                waiting.getCreatedAt(),
                waiting.getId()
        );
        return Math.toIntExact(previousWaitingCount + 1);
    }

    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        Member member = findMember(loginMember);
        return waitingRepository.findAllByMemberOrderByDateAscTimeStartAtAscCreatedAtAsc(member)
                .stream()
                .map(waiting -> MyReservationResponse.fromWaiting(waiting, calculateRank(waiting)))
                .toList();
    }

    @Transactional
    public void cancel(LoginMember loginMember, Long waitingId) {
        Member member = findMember(loginMember);
        Waiting waiting = waitingRepository.findByIdAndMember(waitingId, member)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.WAITING_NOT_FOUND, "대기를 찾을 수 없습니다."));

        waitingRepository.delete(waiting);
    }
}

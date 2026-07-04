package roomescape.service;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;
import roomescape.dto.ReservationRequest;
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
    public WaitingResponse create(LoginMember loginMember, ReservationRequest request) {
        Member member = findMember(loginMember);
        Theme theme = findTheme(request.themeId());
        ReservationTime time = findTime(request.timeId());
        LocalDateTime now = LocalDateTime.now(clock);
        Waiting waiting = new Waiting(member, theme, time, request.date(), now);

        if (waiting.isPast(now)) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간은 대기할 수 없습니다.");
        }
        if (!reservationRepository.existsByThemeAndTimeAndDate(theme, time, request.date())) {
            throw new RoomescapeException(ErrorCode.RESERVATION_SLOT_NOT_OCCUPIED, "예약되지 않은 시간에는 대기할 수 없습니다.");
        }
        if (waitingRepository.existsByMemberAndThemeAndTimeAndDate(member, theme, time, request.date())) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 중인 시간입니다.");
        }

        try {
            Waiting saved = waitingRepository.saveAndFlush(waiting);
            return WaitingResponse.of(saved, calculateRank(saved));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 중인 시간입니다.");
        }
    }

    public int calculateRank(Waiting waiting) {
        long ahead = waitingRepository.countAhead(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate(),
                waiting.getCreatedAt(),
                waiting.getId()
        );
        return Math.toIntExact(ahead + 1);
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
}

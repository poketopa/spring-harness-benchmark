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
        Waiting waiting = new Waiting(member, theme, time, request.date());

        validateCreateAllowed(waiting);
        long rank = waitingRepository.countByThemeAndTimeAndDate(theme, time, request.date()) + 1;

        try {
            return WaitingResponse.of(waitingRepository.saveAndFlush(waiting), rank);
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 신청한 예약 대기입니다.");
        }
    }

    private void validateCreateAllowed(Waiting waiting) {
        if (waiting.isPast(LocalDateTime.now(clock))) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간은 대기할 수 없습니다.");
        }
        if (!reservationRepository.existsByThemeAndTimeAndDate(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate()
        )) {
            throw new RoomescapeException(ErrorCode.WAITING_NOT_AVAILABLE, "예약된 시간에만 대기할 수 있습니다.");
        }
        if (waitingRepository.existsByMemberAndThemeAndTimeAndDate(
                waiting.getMember(),
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate()
        )) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 신청한 예약 대기입니다.");
        }
    }

    @Transactional
    public void delete(LoginMember loginMember, Long id) {
        Member member = findMember(loginMember);
        Waiting waiting = waitingRepository.findByIdAndMember(id, member)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.WAITING_NOT_FOUND, "예약 대기를 찾을 수 없습니다."));

        waitingRepository.delete(waiting);
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

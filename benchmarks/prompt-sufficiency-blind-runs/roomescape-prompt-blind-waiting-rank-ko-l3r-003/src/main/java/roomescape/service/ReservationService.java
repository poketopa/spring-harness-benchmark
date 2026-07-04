package roomescape.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
import roomescape.dto.ReservationMineResponse;
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
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
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ThemeRepository themeRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final WaitingRepository waitingRepository;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            MemberRepository memberRepository,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            WaitingRepository waitingRepository,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
        this.themeRepository = themeRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.waitingRepository = waitingRepository;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(LoginMember loginMember, ReservationRequest request) {
        Member member = findMember(loginMember);
        Theme theme = findTheme(request.themeId());
        ReservationTime time = findTime(request.timeId());
        Reservation reservation = new Reservation(member, theme, time, request.date());

        if (reservation.isPast(LocalDateTime.now(clock))) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간은 예약할 수 없습니다.");
        }
        if (reservationRepository.existsByThemeAndTimeAndDate(theme, time, request.date())) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }

        try {
            return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }

    @Transactional
    public WaitingResponse createWaiting(LoginMember loginMember, ReservationRequest request) {
        Member member = findMember(loginMember);
        Theme theme = findTheme(request.themeId());
        ReservationTime time = findTime(request.timeId());
        Waiting waiting = new Waiting(member, theme, time, request.date(), LocalDateTime.now(clock));

        if (waiting.isPast(LocalDateTime.now(clock))) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간은 대기할 수 없습니다.");
        }
        Reservation reservation = reservationRepository.findByThemeAndTimeAndDate(theme, time, request.date())
                .orElseThrow(() -> new RoomescapeException(
                        ErrorCode.AVAILABLE_SLOT_WAITING,
                        "예약 가능한 시간은 대기할 수 없습니다."
                ));
        if (reservation.isOwnedBy(member)) {
            throw new RoomescapeException(ErrorCode.OWN_RESERVATION_WAITING, "본인의 예약에는 대기할 수 없습니다.");
        }
        if (waitingRepository.existsByMemberAndThemeAndTimeAndDate(member, theme, time, request.date())) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기를 신청한 시간입니다.");
        }

        try {
            Waiting savedWaiting = waitingRepository.saveAndFlush(waiting);
            return WaitingResponse.of(savedWaiting, calculateRank(savedWaiting));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기를 신청한 시간입니다.");
        }
    }

    public List<ReservationMineResponse> findMine(LoginMember loginMember) {
        Member member = findMember(loginMember);

        List<ReservationMineResponse> responses = new ArrayList<>();
        responses.addAll(reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(ReservationMineResponse::from)
                .toList());
        responses.addAll(waitingRepository.findAllByMember(member)
                .stream()
                .map(waiting -> ReservationMineResponse.of(waiting, calculateRank(waiting)))
                .toList());

        return responses.stream()
                .sorted(Comparator.comparing(ReservationMineResponse::date)
                        .thenComparing(ReservationMineResponse::startAt)
                        .thenComparing(ReservationMineResponse::status)
                        .thenComparing(ReservationMineResponse::id))
                .toList();
    }

    private int calculateRank(Waiting waiting) {
        List<Waiting> waitings = waitingRepository.findAllByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate()
        );
        for (int index = 0; index < waitings.size(); index++) {
            if (waitings.get(index).getId().equals(waiting.getId())) {
                return index + 1;
            }
        }
        throw new RoomescapeException(ErrorCode.INVALID_INPUT, "대기 순번을 계산할 수 없습니다.");
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

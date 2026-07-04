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
import roomescape.domain.Waiting;
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
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
        return reservationRepository.findByThemeAndTimeAndDate(theme, time, request.date())
                .map(existing -> createWaiting(member, theme, time, request.date(), existing))
                .orElseGet(() -> createReservation(reservation));
    }

    private ReservationResponse createReservation(Reservation reservation) {
        try {
            return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }

    private ReservationResponse createWaiting(
            Member member,
            Theme theme,
            ReservationTime time,
            LocalDate date,
            Reservation reservation
    ) {
        if (reservation.isOwnedBy(member)) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약한 시간입니다.");
        }
        if (waitingRepository.existsByMemberAndThemeAndTimeAndDate(member, theme, time, date)) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 중인 예약입니다.");
        }

        Waiting waiting = new Waiting(member, theme, time, date);
        try {
            Waiting saved = waitingRepository.saveAndFlush(waiting);
            return ReservationResponse.from(saved, waitingRank(saved));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 중인 예약입니다.");
        }
    }

    public List<ReservationResponse> findMine(LoginMember loginMember) {
        Member member = findMember(loginMember);

        List<ReservationResponse> reservations = reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(ReservationResponse::from)
                .toList();
        List<ReservationResponse> waitings = waitingRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(waiting -> ReservationResponse.from(waiting, waitingRank(waiting)))
                .toList();

        return java.util.stream.Stream.concat(reservations.stream(), waitings.stream())
                .sorted(java.util.Comparator
                        .comparing(ReservationResponse::date)
                        .thenComparing(ReservationResponse::startAt)
                        .thenComparing(ReservationResponse::status))
                .toList();
    }

    private long waitingRank(Waiting waiting) {
        return waitingRepository.countByThemeAndTimeAndDateAndIdLessThanEqual(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate(),
                waiting.getId()
        );
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

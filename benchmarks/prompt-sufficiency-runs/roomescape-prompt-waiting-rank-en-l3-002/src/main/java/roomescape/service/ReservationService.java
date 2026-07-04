package roomescape.service;

import java.time.Clock;
import java.time.LocalDateTime;
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
import roomescape.dto.MyReservationResponse;
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;
    private final ThemeRepository themeRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final WaitingService waitingService;
    private final AuthenticatedMemberService authenticatedMemberService;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            WaitingRepository waitingRepository,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            WaitingService waitingService,
            AuthenticatedMemberService authenticatedMemberService,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.waitingRepository = waitingRepository;
        this.themeRepository = themeRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.waitingService = waitingService;
        this.authenticatedMemberService = authenticatedMemberService;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(LoginMember loginMember, ReservationRequest request) {
        Member member = authenticatedMemberService.findMember(loginMember);
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

    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);

        List<MyReservationResponse> reservations = reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(MyReservationResponse::reserved)
                .toList();
        List<MyReservationResponse> waitings = waitingRepository.findAllByMemberOrderByDateAscTimeStartAtAscCreatedAtAscIdAsc(member)
                .stream()
                .map(this::waitingResponse)
                .toList();

        return java.util.stream.Stream.concat(reservations.stream(), waitings.stream())
                .sorted(Comparator.comparing(MyReservationResponse::date)
                        .thenComparing(MyReservationResponse::startAt)
                        .thenComparing(response -> response.status().name()))
                .toList();
    }

    private Theme findTheme(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.THEME_NOT_FOUND, "테마를 찾을 수 없습니다."));
    }

    private ReservationTime findTime(Long timeId) {
        return reservationTimeRepository.findById(timeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.RESERVATION_TIME_NOT_FOUND, "예약 시간을 찾을 수 없습니다."));
    }

    private MyReservationResponse waitingResponse(Waiting waiting) {
        return MyReservationResponse.waiting(waiting, waitingService.rankOf(waiting));
    }
}

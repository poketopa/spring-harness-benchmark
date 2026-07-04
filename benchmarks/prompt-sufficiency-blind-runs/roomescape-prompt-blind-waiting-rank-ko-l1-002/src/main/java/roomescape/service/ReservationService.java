package roomescape.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationStatus;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ThemeRepository themeRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            MemberRepository memberRepository,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
        this.themeRepository = themeRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(LoginMember loginMember, ReservationRequest request) {
        Member member = findMember(loginMember);
        Theme theme = findTheme(request.themeId());
        ReservationTime time = findTime(request.timeId());
        ReservationStatus status = decideStatus(theme, time, request);
        Reservation reservation = new Reservation(member, theme, time, request.date(), status);

        if (reservation.isPast(LocalDateTime.now(clock))) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간은 예약할 수 없습니다.");
        }

        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        return ReservationResponse.from(savedReservation, waitingRankOf(savedReservation));
    }

    public List<ReservationResponse> findMine(LoginMember loginMember) {
        Member member = findMember(loginMember);

        return reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(reservation -> ReservationResponse.from(reservation, waitingRankOf(reservation)))
                .toList();
    }

    private ReservationStatus decideStatus(Theme theme, ReservationTime time, ReservationRequest request) {
        if (reservationRepository.existsByThemeAndTimeAndDate(theme, time, request.date())) {
            return ReservationStatus.WAITING;
        }
        return ReservationStatus.CONFIRMED;
    }

    private Integer waitingRankOf(Reservation reservation) {
        if (reservation.getStatus() != ReservationStatus.WAITING) {
            return null;
        }
        long previousWaitingCount = reservationRepository.countByThemeAndTimeAndDateAndStatusAndIdLessThan(
                reservation.getTheme(),
                reservation.getTime(),
                reservation.getDate(),
                ReservationStatus.WAITING,
                reservation.getId()
        );
        return Math.toIntExact(previousWaitingCount + 1);
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

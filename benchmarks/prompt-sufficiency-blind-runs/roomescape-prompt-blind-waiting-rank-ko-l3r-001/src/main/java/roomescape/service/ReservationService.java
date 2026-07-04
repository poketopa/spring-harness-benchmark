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
import roomescape.domain.ReservationWaiting;
import roomescape.domain.Theme;
import roomescape.dto.MyReservationResponse;
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
import roomescape.dto.ReservationWaitingResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ReservationWaitingRepository;
import roomescape.repository.ThemeRepository;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationWaitingRepository reservationWaitingRepository;
    private final MemberRepository memberRepository;
    private final ThemeRepository themeRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            ReservationWaitingRepository reservationWaitingRepository,
            MemberRepository memberRepository,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationWaitingRepository = reservationWaitingRepository;
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
    public ReservationWaitingResponse createWaiting(LoginMember loginMember, ReservationRequest request) {
        Member member = findMember(loginMember);
        Theme theme = findTheme(request.themeId());
        ReservationTime time = findTime(request.timeId());

        if (LocalDateTime.of(request.date(), time.getStartAt()).isBefore(LocalDateTime.now(clock))) {
            throw new RoomescapeException(ErrorCode.PAST_RESERVATION, "지난 날짜와 시간은 대기할 수 없습니다.");
        }

        Reservation reservation = reservationRepository.findByThemeAndTimeAndDate(theme, time, request.date())
                .orElseThrow(() -> new RoomescapeException(
                        ErrorCode.WAITING_UNAVAILABLE,
                        "예약 가능한 시간에는 대기할 수 없습니다."
                ));
        if (reservation.isOwnedBy(member)) {
            throw new RoomescapeException(ErrorCode.OWN_RESERVATION_WAITING, "본인의 예약에는 대기할 수 없습니다.");
        }
        if (reservationWaitingRepository.existsByMemberAndThemeAndTimeAndDate(member, theme, time, request.date())) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 신청한 시간입니다.");
        }

        ReservationWaiting waiting = new ReservationWaiting(
                member,
                theme,
                time,
                request.date(),
                LocalDateTime.now(clock)
        );
        try {
            ReservationWaiting saved = reservationWaitingRepository.saveAndFlush(waiting);
            return ReservationWaitingResponse.from(saved, rankOf(saved));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 신청한 시간입니다.");
        }
    }

    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        Member member = findMember(loginMember);

        List<MyReservationResponse> responses = new ArrayList<>();
        responses.addAll(reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(MyReservationResponse::from)
                .toList());
        responses.addAll(reservationWaitingRepository.findAllByMember(member)
                .stream()
                .map(waiting -> MyReservationResponse.from(waiting, rankOf(waiting)))
                .toList());

        return responses.stream()
                .sorted(Comparator.comparing(MyReservationResponse::date)
                        .thenComparing(MyReservationResponse::startAt)
                        .thenComparing(MyReservationResponse::themeId)
                        .thenComparing(response -> "RESERVATION".equals(response.status()) ? 0 : 1)
                        .thenComparing(MyReservationResponse::id))
                .toList();
    }

    private int rankOf(ReservationWaiting waiting) {
        List<ReservationWaiting> waitings = reservationWaitingRepository
                .findAllByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(
                        waiting.getTheme(),
                        waiting.getTime(),
                        waiting.getDate()
                );
        for (int index = 0; index < waitings.size(); index++) {
            if (waitings.get(index).getId().equals(waiting.getId())) {
                return index + 1;
            }
        }
        throw new RoomescapeException(ErrorCode.WAITING_UNAVAILABLE, "대기 순번을 계산할 수 없습니다.");
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

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
import roomescape.dto.ReservationRequest;
import roomescape.dto.ReservationResponse;
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
    private final MemberRepository memberRepository;
    private final ThemeRepository themeRepository;
    private final ReservationTimeRepository reservationTimeRepository;
    private final ReservationWaitingRepository reservationWaitingRepository;
    private final Clock clock;

    public ReservationService(
            ReservationRepository reservationRepository,
            MemberRepository memberRepository,
            ThemeRepository themeRepository,
            ReservationTimeRepository reservationTimeRepository,
            ReservationWaitingRepository reservationWaitingRepository,
            Clock clock
    ) {
        this.reservationRepository = reservationRepository;
        this.memberRepository = memberRepository;
        this.themeRepository = themeRepository;
        this.reservationTimeRepository = reservationTimeRepository;
        this.reservationWaitingRepository = reservationWaitingRepository;
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
        if (reservationRepository.findByThemeAndTimeAndDate(theme, time, request.date()).isPresent()) {
            return createWaiting(member, theme, time, request);
        }

        try {
            return ReservationResponse.from(reservationRepository.saveAndFlush(reservation));
        } catch (DataIntegrityViolationException exception) {
            return createWaiting(member, theme, time, request);
        }
    }

    public List<ReservationResponse> findMine(LoginMember loginMember) {
        Member member = findMember(loginMember);

        List<ReservationResponse> responses = new ArrayList<>(reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(ReservationResponse::from)
                .toList());
        responses.addAll(reservationWaitingRepository.findAllByMemberOrderByDateAscTimeStartAtAscCreatedAtAsc(member)
                .stream()
                .map(waiting -> ReservationResponse.waiting(waiting, waitingRank(waiting)))
                .toList());

        return responses.stream()
                .sorted(Comparator.comparing(ReservationResponse::date)
                        .thenComparing(ReservationResponse::startAt)
                        .thenComparing(response -> response.waitingRank() == null ? 0 : response.waitingRank()))
                .toList();
    }

    private ReservationResponse createWaiting(Member member, Theme theme, ReservationTime time, ReservationRequest request) {
        if (reservationRepository.findByThemeAndTimeAndDate(theme, time, request.date())
                .filter(reservation -> reservation.isOwnedBy(member))
                .isPresent()) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약한 시간입니다.");
        }
        if (reservationWaitingRepository.existsByMemberAndThemeAndTimeAndDate(member, theme, time, request.date())) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 중인 시간입니다.");
        }

        ReservationWaiting waiting = new ReservationWaiting(member, theme, time, request.date(), LocalDateTime.now(clock));
        try {
            ReservationWaiting saved = reservationWaitingRepository.saveAndFlush(waiting);
            long waitingCount = reservationWaitingRepository.countByThemeAndTimeAndDate(theme, time, request.date());
            return ReservationResponse.waiting(saved, Math.toIntExact(waitingCount));
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_WAITING, "이미 대기 중인 시간입니다.");
        }
    }

    private int waitingRank(ReservationWaiting waiting) {
        List<ReservationWaiting> waitings = reservationWaitingRepository.findAllByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate()
        );

        for (int index = 0; index < waitings.size(); index++) {
            if (waitings.get(index).getId().equals(waiting.getId())) {
                return index + 1;
            }
        }
        throw new RoomescapeException(ErrorCode.INVALID_INPUT, "대기 순서를 계산할 수 없습니다.");
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

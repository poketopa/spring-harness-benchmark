package roomescape.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Waiting;
import roomescape.dto.MyReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class MyReservationService {

    private static final Comparator<MyReservationResponse> MY_RESERVATION_ORDER = Comparator
            .comparing(MyReservationResponse::date)
            .thenComparing(MyReservationResponse::startAt)
            .thenComparing(MyReservationResponse::status);

    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;

    public MyReservationService(
            MemberRepository memberRepository,
            ReservationRepository reservationRepository,
            WaitingRepository waitingRepository
    ) {
        this.memberRepository = memberRepository;
        this.reservationRepository = reservationRepository;
        this.waitingRepository = waitingRepository;
    }

    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        Member member = findMember(loginMember);
        List<MyReservationResponse> reservations = reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(MyReservationResponse::fromReservation)
                .toList();
        List<MyReservationResponse> waitings = waitingRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(waiting -> MyReservationResponse.ofWaiting(waiting, rankOf(waiting)))
                .toList();

        return merge(reservations, waitings);
    }

    private long rankOf(Waiting waiting) {
        return waitingRepository.countByThemeAndTimeAndDateAndIdLessThan(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate(),
                waiting.getId()
        ) + 1;
    }

    private List<MyReservationResponse> merge(
            List<MyReservationResponse> reservations,
            List<MyReservationResponse> waitings
    ) {
        return Stream.concat(reservations.stream(), waitings.stream())
                .sorted(MY_RESERVATION_ORDER)
                .toList();
    }

    private Member findMember(LoginMember loginMember) {
        return memberRepository.findById(loginMember.id())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
    }
}

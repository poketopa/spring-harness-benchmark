package roomescape.service;

import java.util.Comparator;
import java.util.List;
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

    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;
    private final MemberRepository memberRepository;

    public MyReservationService(
            ReservationRepository reservationRepository,
            WaitingRepository waitingRepository,
            MemberRepository memberRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.waitingRepository = waitingRepository;
        this.memberRepository = memberRepository;
    }

    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        Member member = findMember(loginMember);

        List<MyReservationResponse> reservations = reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(MyReservationResponse::fromReservation)
                .toList();
        List<MyReservationResponse> waitings = waitingRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(waiting -> MyReservationResponse.ofWaiting(waiting, calculateRank(waiting)))
                .toList();

        return combineAndSort(reservations, waitings);
    }

    private long calculateRank(Waiting waiting) {
        return waitingRepository.countEarlierWaitings(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate(),
                waiting.getCreatedAt(),
                waiting.getId()
        ) + 1;
    }

    private List<MyReservationResponse> combineAndSort(
            List<MyReservationResponse> reservations,
            List<MyReservationResponse> waitings
    ) {
        return java.util.stream.Stream.concat(reservations.stream(), waitings.stream())
                .sorted(Comparator.comparing(MyReservationResponse::date)
                        .thenComparing(MyReservationResponse::startAt)
                        .thenComparing(MyReservationResponse::status))
                .toList();
    }

    private Member findMember(LoginMember loginMember) {
        return memberRepository.findById(loginMember.id())
                .orElseThrow(() -> new RoomescapeException(ErrorCode.UNAUTHORIZED, "유효하지 않은 인증 정보입니다."));
    }
}

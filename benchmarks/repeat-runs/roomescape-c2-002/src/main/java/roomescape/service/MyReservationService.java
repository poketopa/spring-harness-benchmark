package roomescape.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Waiting;
import roomescape.dto.MyReservationResponse;
import roomescape.repository.ReservationRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class MyReservationService {

    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;
    private final AuthenticatedMemberService authenticatedMemberService;

    public MyReservationService(
            ReservationRepository reservationRepository,
            WaitingRepository waitingRepository,
            AuthenticatedMemberService authenticatedMemberService
    ) {
        this.reservationRepository = reservationRepository;
        this.waitingRepository = waitingRepository;
        this.authenticatedMemberService = authenticatedMemberService;
    }

    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        Member member = authenticatedMemberService.getMember(loginMember);
        List<MyReservationResponse> reservations = reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(MyReservationResponse::fromReservation)
                .toList();
        List<MyReservationResponse> waitings = waitingRepository.findAllByMemberOrderByDateAscTimeStartAtAscCreatedAtAsc(member)
                .stream()
                .map(waiting -> MyReservationResponse.ofWaiting(waiting, calculateRank(waiting)))
                .toList();

        return java.util.stream.Stream.concat(reservations.stream(), waitings.stream())
                .sorted(Comparator.comparing(MyReservationResponse::date)
                        .thenComparing(MyReservationResponse::startAt)
                        .thenComparing(MyReservationResponse::status))
                .toList();
    }

    private int calculateRank(Waiting waiting) {
        long previousWaitingCount = waitingRepository.countEarlierWaitings(
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate(),
                waiting.getCreatedAt(),
                waiting.getId()
        );
        return Math.toIntExact(previousWaitingCount + 1);
    }
}

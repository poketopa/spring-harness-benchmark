package roomescape.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.dto.MyReservationResponse;
import roomescape.repository.ReservationRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class MyReservationService {

    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;
    private final AuthenticatedMemberService authenticatedMemberService;
    private final WaitingRankService waitingRankService;

    public MyReservationService(
            ReservationRepository reservationRepository,
            WaitingRepository waitingRepository,
            AuthenticatedMemberService authenticatedMemberService,
            WaitingRankService waitingRankService
    ) {
        this.reservationRepository = reservationRepository;
        this.waitingRepository = waitingRepository;
        this.authenticatedMemberService = authenticatedMemberService;
        this.waitingRankService = waitingRankService;
    }

    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        Member member = authenticatedMemberService.findMember(loginMember);
        List<MyReservationResponse> reservations = findReservations(member);
        List<MyReservationResponse> waitings = findWaitings(member);

        return java.util.stream.Stream.concat(reservations.stream(), waitings.stream())
                .sorted(Comparator.comparing(MyReservationResponse::date)
                        .thenComparing(MyReservationResponse::startAt)
                        .thenComparing(MyReservationResponse::status))
                .toList();
    }

    private List<MyReservationResponse> findReservations(Member member) {
        return reservationRepository.findAllByMemberOrderByDateAscTimeStartAtAsc(member)
                .stream()
                .map(MyReservationResponse::fromReservation)
                .toList();
    }

    private List<MyReservationResponse> findWaitings(Member member) {
        return waitingRepository.findAllByMemberOrderByDateAscTimeStartAtAscCreatedAtAsc(member)
                .stream()
                .map(waiting -> MyReservationResponse.ofWaiting(waiting, waitingRankService.calculate(waiting)))
                .toList();
    }
}

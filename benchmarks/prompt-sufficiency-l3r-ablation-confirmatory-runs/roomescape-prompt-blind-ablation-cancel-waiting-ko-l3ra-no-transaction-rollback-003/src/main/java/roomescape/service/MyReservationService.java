package roomescape.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import roomescape.auth.LoginMember;
import roomescape.dto.MyReservationResponse;

@Service
public class MyReservationService {

    private final ReservationService reservationService;
    private final WaitingService waitingService;

    public MyReservationService(ReservationService reservationService, WaitingService waitingService) {
        this.reservationService = reservationService;
        this.waitingService = waitingService;
    }

    public List<MyReservationResponse> findMine(LoginMember loginMember) {
        List<MyReservationResponse> reservations = reservationService.findMine(loginMember);
        List<MyReservationResponse> waitings = waitingService.findMine(loginMember);

        return java.util.stream.Stream.concat(reservations.stream(), waitings.stream())
                .sorted(Comparator.comparing(MyReservationResponse::date)
                        .thenComparing(MyReservationResponse::startAt)
                        .thenComparing(MyReservationResponse::status))
                .toList();
    }
}

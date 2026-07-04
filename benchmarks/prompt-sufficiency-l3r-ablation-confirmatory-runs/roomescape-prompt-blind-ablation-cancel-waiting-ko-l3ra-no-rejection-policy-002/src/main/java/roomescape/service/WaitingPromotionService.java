package roomescape.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Reservation;
import roomescape.domain.Waiting;
import roomescape.repository.ReservationRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class WaitingPromotionService {

    private final WaitingRepository waitingRepository;
    private final ReservationRepository reservationRepository;

    public WaitingPromotionService(
            WaitingRepository waitingRepository,
            ReservationRepository reservationRepository
    ) {
        this.waitingRepository = waitingRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void promoteFirstWaiting(Reservation canceledReservation) {
        waitingRepository.findFirstByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(
                        canceledReservation.getTheme(),
                        canceledReservation.getTime(),
                        canceledReservation.getDate()
                )
                .ifPresent(this::promote);
    }

    private void promote(Waiting waiting) {
        waitingRepository.delete(waiting);
        reservationRepository.saveAndFlush(new Reservation(
                waiting.getMember(),
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate()
        ));
    }
}

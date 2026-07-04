package roomescape.service;

import org.springframework.stereotype.Service;
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

    public WaitingPromotionService(WaitingRepository waitingRepository, ReservationRepository reservationRepository) {
        this.waitingRepository = waitingRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public void promoteNext(Reservation canceledReservation) {
        waitingRepository.findFirstByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(
                        canceledReservation.getTheme(),
                        canceledReservation.getTime(),
                        canceledReservation.getDate()
                )
                .ifPresent(this::promote);
    }

    private void promote(Waiting waiting) {
        waitingRepository.delete(waiting);
        waitingRepository.flush();

        Reservation promotedReservation = new Reservation(
                waiting.getMember(),
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate()
        );
        reservationRepository.saveAndFlush(promotedReservation);
    }
}

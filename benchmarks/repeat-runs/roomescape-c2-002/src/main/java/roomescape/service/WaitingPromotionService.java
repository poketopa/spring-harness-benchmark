package roomescape.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
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
    public void promoteFirstWaiting(Theme theme, ReservationTime time, LocalDate date) {
        waitingRepository.findFirstByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(theme, time, date)
                .ifPresent(this::promote);
    }

    private void promote(Waiting waiting) {
        Reservation reservation = waiting.approve();
        waitingRepository.delete(waiting);
        reservationRepository.save(reservation);
    }
}

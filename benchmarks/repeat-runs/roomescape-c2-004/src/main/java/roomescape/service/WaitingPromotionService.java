package roomescape.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Reservation;
import roomescape.domain.Waiting;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class WaitingPromotionService {

    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;

    public WaitingPromotionService(ReservationRepository reservationRepository, WaitingRepository waitingRepository) {
        this.reservationRepository = reservationRepository;
        this.waitingRepository = waitingRepository;
    }

    @Transactional
    public void cancelAndPromote(Reservation reservation) {
        waitingRepository.findFirstByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(
                        reservation.getTheme(),
                        reservation.getTime(),
                        reservation.getDate()
                )
                .ifPresentOrElse(
                        waiting -> replaceReservationWithWaiting(reservation, waiting),
                        () -> reservationRepository.delete(reservation)
                );
    }

    private void replaceReservationWithWaiting(Reservation reservation, Waiting waiting) {
        reservationRepository.delete(reservation);
        reservationRepository.flush();
        waitingRepository.delete(waiting);

        Reservation promotedReservation = new Reservation(
                waiting.getMember(),
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate()
        );
        try {
            reservationRepository.saveAndFlush(promotedReservation);
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "이미 예약된 시간입니다.");
        }
    }
}

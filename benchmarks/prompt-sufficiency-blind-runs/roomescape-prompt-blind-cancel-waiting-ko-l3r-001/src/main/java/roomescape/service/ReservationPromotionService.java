package roomescape.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import roomescape.domain.Reservation;
import roomescape.domain.Waiting;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;

@Service
public class ReservationPromotionService {

    private final ReservationRepository reservationRepository;

    public ReservationPromotionService(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    public void promote(Waiting waiting) {
        Reservation reservation = new Reservation(
                waiting.getMember(),
                waiting.getTheme(),
                waiting.getTime(),
                waiting.getDate()
        );

        try {
            reservationRepository.saveAndFlush(reservation);
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.DUPLICATE_RESERVATION, "대기 예약 승격에 실패했습니다.");
        }
    }
}

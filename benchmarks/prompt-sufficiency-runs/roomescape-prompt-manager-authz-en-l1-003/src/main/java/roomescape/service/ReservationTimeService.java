package roomescape.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.dto.ReservationTimeRequest;
import roomescape.dto.ReservationTimeResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;

@Service
@Transactional(readOnly = true)
public class ReservationTimeService {

    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;
    private final ReservationRepository reservationRepository;
    private final WaitingRepository waitingRepository;

    public ReservationTimeService(
            ReservationTimeRepository reservationTimeRepository,
            ThemeRepository themeRepository,
            ReservationRepository reservationRepository,
            WaitingRepository waitingRepository
    ) {
        this.reservationTimeRepository = reservationTimeRepository;
        this.themeRepository = themeRepository;
        this.reservationRepository = reservationRepository;
        this.waitingRepository = waitingRepository;
    }

    @Transactional
    public ReservationTimeResponse create(ReservationTimeRequest request) {
        ReservationTime time = reservationTimeRepository.save(new ReservationTime(request.startAt()));
        return ReservationTimeResponse.of(time, false);
    }

    public List<ReservationTimeResponse> findThemeTimes(Long themeId, LocalDate date) {
        Theme theme = themeRepository.getByIdOrThrow(themeId);
        Set<Long> reservedTimeIds = findReservedTimeIds(theme, date);
        return reservationTimeRepository.findAll()
                .stream()
                .map(time -> ReservationTimeResponse.of(time, reservedTimeIds.contains(time.getId())))
                .toList();
    }

    private Set<Long> findReservedTimeIds(Theme theme, LocalDate date) {
        return reservationRepository.findAllByThemeAndDate(theme, date)
                .stream()
                .map(Reservation::getTime)
                .map(ReservationTime::getId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void delete(Long timeId) {
        ReservationTime time = reservationTimeRepository.getByIdOrThrow(timeId);
        validateTimeNotInUse(time);

        try {
            reservationTimeRepository.delete(time);
            reservationTimeRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new RoomescapeException(ErrorCode.RESERVATION_TIME_IN_USE, "예약 또는 대기가 존재하는 시간은 삭제할 수 없습니다.");
        }
    }

    private void validateTimeNotInUse(ReservationTime time) {
        if (reservationRepository.existsByTime(time) || waitingRepository.existsByTime(time)) {
            throw new RoomescapeException(ErrorCode.RESERVATION_TIME_IN_USE, "예약 또는 대기가 존재하는 시간은 삭제할 수 없습니다.");
        }
    }
}

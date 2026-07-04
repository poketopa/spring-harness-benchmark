package roomescape.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.domain.ReservationTime;
import roomescape.domain.ReservationStatus;
import roomescape.domain.Theme;
import roomescape.dto.ReservationTimeRequest;
import roomescape.dto.ReservationTimeResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;

@Service
@Transactional(readOnly = true)
public class ReservationTimeService {

    private final ReservationTimeRepository reservationTimeRepository;
    private final ThemeRepository themeRepository;
    private final ReservationRepository reservationRepository;

    public ReservationTimeService(
            ReservationTimeRepository reservationTimeRepository,
            ThemeRepository themeRepository,
            ReservationRepository reservationRepository
    ) {
        this.reservationTimeRepository = reservationTimeRepository;
        this.themeRepository = themeRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public ReservationTimeResponse create(ReservationTimeRequest request) {
        ReservationTime time = reservationTimeRepository.save(new ReservationTime(request.startAt()));
        return ReservationTimeResponse.of(time, false);
    }

    public List<ReservationTimeResponse> findThemeTimes(Long themeId, LocalDate date) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new RoomescapeException(ErrorCode.THEME_NOT_FOUND, "테마를 찾을 수 없습니다."));
        return reservationTimeRepository.findAll()
                .stream()
                .map(time -> ReservationTimeResponse.of(
                        time,
                        reservationRepository.existsByThemeAndTimeAndDateAndStatus(
                                theme,
                                time,
                                date,
                                ReservationStatus.CONFIRMED
                        )
                ))
                .toList();
    }
}

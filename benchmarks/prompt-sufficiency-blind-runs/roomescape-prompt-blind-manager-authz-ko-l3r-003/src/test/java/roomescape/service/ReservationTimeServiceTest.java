package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.domain.ReservationTime;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;

class ReservationTimeServiceTest {

    private final ReservationTimeRepository reservationTimeRepository = mock(ReservationTimeRepository.class);
    private final ThemeRepository themeRepository = mock(ThemeRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final WaitingRepository waitingRepository = mock(WaitingRepository.class);
    private final ReservationTimeService reservationTimeService = new ReservationTimeService(
            reservationTimeRepository,
            themeRepository,
            storeRepository,
            reservationRepository,
            waitingRepository
    );

    @Test
    @DisplayName("대기가 걸린 예약 시간은 삭제할 수 없다")
    void deleteWaitingTimeThrowsReservationTimeInUse() {
        // given
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        when(reservationTimeRepository.getByIdOrThrow(1L)).thenReturn(time);
        when(reservationRepository.existsByTime(time)).thenReturn(false);
        when(waitingRepository.existsByTime(time)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> reservationTimeService.delete(1L))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_TIME_IN_USE));
    }
}

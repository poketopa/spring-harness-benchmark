package roomescape.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.dto.ReservationRequest;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

class ReservationServiceTest {

    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ThemeRepository themeRepository = mock(ThemeRepository.class);
    private final ReservationTimeRepository reservationTimeRepository = mock(ReservationTimeRepository.class);
    private final AuthenticatedMemberService authenticatedMemberService = mock(AuthenticatedMemberService.class);
    private final ManagerAuthorizationService managerAuthorizationService = mock(ManagerAuthorizationService.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final WaitingPromotionService waitingPromotionService = mock(WaitingPromotionService.class);

    @Test
    @DisplayName("지난 날짜와 시간으로 예약을 생성할 수 없다")
    void createPastReservationThrowsPastReservation() {
        // given
        ReservationService reservationService = reservationService(LocalDateTime.of(2030, 5, 1, 10, 1));
        LoginMember loginMember = new LoginMember(1L, "브라운");
        Member member = new Member("브라운", "brown@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        ReservationRequest request = new ReservationRequest(LocalDate.of(2030, 5, 1), 1L, 1L);
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(member);
        when(themeRepository.getByIdOrThrow(1L)).thenReturn(theme);
        when(reservationTimeRepository.getByIdOrThrow(1L)).thenReturn(time);

        // when & then
        assertThatThrownBy(() -> reservationService.create(loginMember, request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAST_RESERVATION));
    }

    @Test
    @DisplayName("이미 예약된 슬롯으로 예약을 생성할 수 없다")
    void createDuplicateReservationThrowsDuplicateReservation() {
        // given
        ReservationService reservationService = reservationService(LocalDateTime.of(2030, 5, 1, 9, 0));
        LoginMember loginMember = new LoginMember(1L, "브라운");
        Member member = new Member("브라운", "brown@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        ReservationRequest request = new ReservationRequest(date, 1L, 1L);
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(member);
        when(themeRepository.getByIdOrThrow(1L)).thenReturn(theme);
        when(reservationTimeRepository.getByIdOrThrow(1L)).thenReturn(time);
        when(reservationRepository.existsByThemeAndTimeAndDate(theme, time, date)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> reservationService.create(loginMember, request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_RESERVATION));
    }

    private ReservationService reservationService(LocalDateTime now) {
        return new ReservationService(
                reservationRepository,
                themeRepository,
                reservationTimeRepository,
                authenticatedMemberService,
                managerAuthorizationService,
                storeRepository,
                waitingPromotionService,
                fixedClock(now)
        );
    }

    private Clock fixedClock(LocalDateTime now) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        return Clock.fixed(now.atZone(zone).toInstant(), zone);
    }
}

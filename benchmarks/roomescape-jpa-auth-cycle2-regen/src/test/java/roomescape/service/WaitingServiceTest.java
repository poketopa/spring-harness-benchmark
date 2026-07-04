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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.dto.WaitingRequest;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;

class WaitingServiceTest {

    private final WaitingRepository waitingRepository = mock(WaitingRepository.class);
    private final ReservationRepository reservationRepository = mock(ReservationRepository.class);
    private final ThemeRepository themeRepository = mock(ThemeRepository.class);
    private final ReservationTimeRepository reservationTimeRepository = mock(ReservationTimeRepository.class);
    private final AuthenticatedMemberService authenticatedMemberService = mock(AuthenticatedMemberService.class);
    private final WaitingRankService waitingRankService = mock(WaitingRankService.class);
    private final AdminAuthorizationService adminAuthorizationService = mock(AdminAuthorizationService.class);

    @Test
    @DisplayName("예약되지 않은 슬롯에는 대기할 수 없다")
    void createWaitingWithoutReservationThrowsWaitingNotAllowed() {
        WaitingService waitingService = waitingService(LocalDateTime.of(2030, 5, 1, 9, 0));
        LoginMember loginMember = new LoginMember(1L, "코니");
        Member member = new Member("코니", "cony@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        WaitingRequest request = new WaitingRequest(date, 1L, 1L);
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(member);
        when(themeRepository.getByIdOrThrow(1L)).thenReturn(theme);
        when(reservationTimeRepository.getByIdOrThrow(1L)).thenReturn(time);
        when(reservationRepository.findByThemeAndTimeAndDate(theme, time, date)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> waitingService.create(loginMember, request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WAITING_NOT_ALLOWED));
    }

    @Test
    @DisplayName("본인의 예약에는 대기할 수 없다")
    void createWaitingForOwnReservationThrowsWaitingNotAllowed() {
        WaitingService waitingService = waitingService(LocalDateTime.of(2030, 5, 1, 9, 0));
        LoginMember loginMember = new LoginMember(1L, "브라운");
        Member member = new Member("브라운", "brown@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        WaitingRequest request = new WaitingRequest(date, 1L, 1L);
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(member);
        when(themeRepository.getByIdOrThrow(1L)).thenReturn(theme);
        when(reservationTimeRepository.getByIdOrThrow(1L)).thenReturn(time);
        when(reservationRepository.findByThemeAndTimeAndDate(theme, time, date))
                .thenReturn(Optional.of(new Reservation(member, theme, time, date)));

        assertThatThrownBy(() -> waitingService.create(loginMember, request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.WAITING_NOT_ALLOWED));
    }

    @Test
    @DisplayName("지난 날짜와 시간에는 대기할 수 없다")
    void createPastWaitingThrowsPastReservation() {
        WaitingService waitingService = waitingService(LocalDateTime.of(2030, 5, 1, 10, 1));
        LoginMember loginMember = new LoginMember(1L, "코니");
        Member member = new Member("코니", "cony@example.com", "password");
        Member reservedMember = new Member("브라운", "brown@example.com", "password");
        Theme theme = new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg");
        ReservationTime time = new ReservationTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        WaitingRequest request = new WaitingRequest(date, 1L, 1L);
        when(authenticatedMemberService.findMember(loginMember)).thenReturn(member);
        when(themeRepository.getByIdOrThrow(1L)).thenReturn(theme);
        when(reservationTimeRepository.getByIdOrThrow(1L)).thenReturn(time);
        when(reservationRepository.findByThemeAndTimeAndDate(theme, time, date))
                .thenReturn(Optional.of(new Reservation(reservedMember, theme, time, date)));

        assertThatThrownBy(() -> waitingService.create(loginMember, request))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAST_RESERVATION));
    }

    private WaitingService waitingService(LocalDateTime now) {
        return new WaitingService(
                waitingRepository,
                reservationRepository,
                themeRepository,
                reservationTimeRepository,
                authenticatedMemberService,
                waitingRankService,
                adminAuthorizationService,
                fixedClock(now)
        );
    }

    private Clock fixedClock(LocalDateTime now) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        return Clock.fixed(now.atZone(zone).toInstant(), zone);
    }
}

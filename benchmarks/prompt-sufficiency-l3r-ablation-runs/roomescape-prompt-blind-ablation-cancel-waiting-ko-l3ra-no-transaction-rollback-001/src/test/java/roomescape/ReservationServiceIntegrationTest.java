package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.dto.MyReservationResponse;
import roomescape.dto.ReservationStatus;
import roomescape.dto.WaitingRequest;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.service.MyReservationService;
import roomescape.service.ReservationService;
import roomescape.service.WaitingService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private WaitingService waitingService;

    @Autowired
    private MyReservationService myReservationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("회원은 본인의 미래 예약을 취소할 수 있다")
    void cancelOwnFutureReservation() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));

        reservationService.cancel(loginMember(brown), reservation.getId());

        assertThat(reservationRepository.findByThemeAndTimeAndDate(theme, time, date)).isEmpty();
    }

    @Test
    @DisplayName("지난 예약은 취소할 수 없다")
    void cancelPastReservationThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        LocalDate date = LocalDate.of(2000, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAST_RESERVATION));
        assertThat(reservationRepository.findByThemeAndTimeAndDate(theme, time, date)).isPresent();
    }

    @Test
    @DisplayName("다른 회원의 예약은 취소할 수 없다")
    void cancelOtherMemberReservationThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(cony), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        assertThat(reservationRepository.findByThemeAndTimeAndDate(theme, time, date)).isPresent();
    }

    @Test
    @DisplayName("예약 취소 시 같은 슬롯의 가장 빠른 대기가 예약으로 승격되고 남은 대기 순번은 다시 계산된다")
    void cancelReservationPromotesFirstWaitingAndRecalculatesRanks() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));
        WaitingRequest request = new WaitingRequest(date, time.getId(), theme.getId());
        waitingService.create(loginMember(cony), request);
        waitingService.create(loginMember(sally), request);

        reservationService.cancel(loginMember(brown), reservation.getId());

        List<MyReservationResponse> conyMine = myReservationService.findMine(loginMember(cony));
        assertThat(conyMine).hasSize(1);
        assertThat(conyMine.getFirst().status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(conyMine.getFirst().rank()).isNull();

        List<MyReservationResponse> sallyMine = myReservationService.findMine(loginMember(sally));
        assertThat(sallyMine).hasSize(1);
        assertThat(sallyMine.getFirst().status()).isEqualTo(ReservationStatus.WAITING);
        assertThat(sallyMine.getFirst().rank()).isEqualTo(1);
    }

    private Member saveMember(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private Theme saveTheme() {
        return themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
    }

    private ReservationTime saveTime() {
        return timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

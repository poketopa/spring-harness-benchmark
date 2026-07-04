package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import roomescape.domain.Waiting;
import roomescape.dto.MyReservationResponse;
import roomescape.dto.ReservationStatus;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;
import roomescape.service.MyReservationService;
import roomescape.service.ReservationService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationCancelServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

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

    @Autowired
    private WaitingRepository waitingRepository;

    @Test
    @DisplayName("회원은 본인의 미래 예약을 취소할 수 있다")
    void cancelOwnFutureReservationDeletesReservation() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        Reservation reservation = saveReservation(brown, theme, time, LocalDate.of(2030, 5, 1));

        reservationService.cancel(loginMember(brown), reservation.getId());

        assertThat(reservationRepository.existsById(reservation.getId())).isFalse();
        assertThat(myReservationService.findMine(loginMember(brown))).isEmpty();
    }

    @Test
    @DisplayName("지난 예약 취소는 거절한다")
    void cancelPastReservationThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        Reservation reservation = saveReservation(brown, theme, time, LocalDate.of(2000, 5, 1));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAST_RESERVATION));
        assertThat(reservationRepository.existsById(reservation.getId())).isTrue();
    }

    @Test
    @DisplayName("다른 회원의 예약 취소는 거절한다")
    void cancelOtherMemberReservationThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        Reservation reservation = saveReservation(brown, theme, time, LocalDate.of(2030, 5, 1));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(cony), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        assertThat(reservationRepository.existsById(reservation.getId())).isTrue();
    }

    @Test
    @DisplayName("예약 취소 시 같은 슬롯의 가장 빠른 대기 1명을 예약으로 승격하고 남은 순번을 다시 계산한다")
    void cancelReservationPromotesFirstWaitingAndRecalculatesRanks() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = saveReservation(brown, theme, time, date);
        Waiting firstWaiting = saveWaiting(cony, theme, time, date, LocalDateTime.of(2030, 1, 1, 10, 0));
        Waiting secondWaiting = saveWaiting(sally, theme, time, date, LocalDateTime.of(2030, 1, 1, 10, 1));

        reservationService.cancel(loginMember(brown), reservation.getId());

        assertThat(reservationRepository.existsById(reservation.getId())).isFalse();
        assertThat(waitingRepository.existsById(firstWaiting.getId())).isFalse();
        assertThat(waitingRepository.existsById(secondWaiting.getId())).isTrue();

        List<MyReservationResponse> conyMine = myReservationService.findMine(loginMember(cony));
        assertThat(conyMine).singleElement().satisfies(response -> {
            assertThat(response.status()).isEqualTo(ReservationStatus.RESERVED);
            assertThat(response.rank()).isNull();
            assertThat(response.date()).isEqualTo(date);
            assertThat(response.timeId()).isEqualTo(time.getId());
            assertThat(response.themeId()).isEqualTo(theme.getId());
        });

        List<MyReservationResponse> sallyMine = myReservationService.findMine(loginMember(sally));
        assertThat(sallyMine).singleElement().satisfies(response -> {
            assertThat(response.status()).isEqualTo(ReservationStatus.WAITING);
            assertThat(response.rank()).isEqualTo(1);
        });
    }

    private Member saveMember(String name, String email) {
        return memberRepository.saveAndFlush(new Member(name, email, "password"));
    }

    private Theme saveTheme() {
        return themeRepository.saveAndFlush(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
    }

    private ReservationTime saveTime() {
        return timeRepository.saveAndFlush(new ReservationTime(LocalTime.of(10, 0)));
    }

    private Reservation saveReservation(Member member, Theme theme, ReservationTime time, LocalDate date) {
        return reservationRepository.saveAndFlush(new Reservation(member, theme, time, date));
    }

    private Waiting saveWaiting(
            Member member,
            Theme theme,
            ReservationTime time,
            LocalDate date,
            LocalDateTime createdAt
    ) {
        return waitingRepository.saveAndFlush(new Waiting(member, theme, time, date, createdAt));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

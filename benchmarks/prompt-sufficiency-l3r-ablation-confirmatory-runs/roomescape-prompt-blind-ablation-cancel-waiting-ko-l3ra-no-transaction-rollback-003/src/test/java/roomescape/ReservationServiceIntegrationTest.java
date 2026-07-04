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
import org.springframework.transaction.annotation.Transactional;
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
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationServiceIntegrationTest {

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
    void memberCancelsOwnReservation() {
        Reservation reservation = saveReservation(saveMember("브라운", "brown@example.com"), LocalDate.of(2030, 5, 1));

        reservationService.cancel(loginMember(reservation.getMember()), reservation.getId());

        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("지난 예약 취소는 거절된다")
    void pastReservationCancelThrowsException() {
        Reservation reservation = saveReservation(saveMember("브라운", "brown@example.com"), LocalDate.of(2000, 5, 1));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(reservation.getMember()), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PAST_RESERVATION));
    }

    @Test
    @DisplayName("다른 회원의 예약 취소는 거절된다")
    void otherMemberReservationCancelThrowsException() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Reservation reservation = saveReservation(brown, LocalDate.of(2030, 5, 1));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(cony), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
    }

    @Test
    @DisplayName("예약 취소 시 같은 슬롯의 가장 빠른 대기가 예약으로 승격된다")
    void cancelReservationPromotesFirstWaiting() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Slot slot = saveSlot();
        Reservation reservation = reservationRepository.save(new Reservation(
                brown,
                slot.theme(),
                slot.time(),
                slot.date()
        ));
        waitingRepository.save(new Waiting(
                cony,
                slot.theme(),
                slot.time(),
                slot.date(),
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));
        waitingRepository.save(new Waiting(
                sally,
                slot.theme(),
                slot.time(),
                slot.date(),
                LocalDateTime.of(2030, 1, 1, 10, 1)
        ));

        reservationService.cancel(loginMember(brown), reservation.getId());

        List<MyReservationResponse> conyReservations = myReservationService.findMine(loginMember(cony));
        assertThat(conyReservations).singleElement()
                .satisfies(response -> {
                    assertThat(response.status()).isEqualTo(ReservationStatus.RESERVED);
                    assertThat(response.rank()).isNull();
                });
    }

    @Test
    @DisplayName("승격 후 남은 대기 순번은 신청 순서 기준으로 다시 계산된다")
    void waitingRankIsRecalculatedAfterPromotion() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Member sally = saveMember("샐리", "sally@example.com");
        Slot slot = saveSlot();
        Reservation reservation = reservationRepository.save(new Reservation(
                brown,
                slot.theme(),
                slot.time(),
                slot.date()
        ));
        waitingRepository.save(new Waiting(
                cony,
                slot.theme(),
                slot.time(),
                slot.date(),
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));
        waitingRepository.save(new Waiting(
                sally,
                slot.theme(),
                slot.time(),
                slot.date(),
                LocalDateTime.of(2030, 1, 1, 10, 1)
        ));

        reservationService.cancel(loginMember(brown), reservation.getId());

        List<MyReservationResponse> sallyReservations = myReservationService.findMine(loginMember(sally));
        assertThat(sallyReservations).singleElement()
                .satisfies(response -> {
                    assertThat(response.status()).isEqualTo(ReservationStatus.WAITING);
                    assertThat(response.rank()).isEqualTo(1);
                });
    }

    private Reservation saveReservation(Member member, LocalDate date) {
        Slot slot = saveSlot(date);
        return reservationRepository.save(new Reservation(member, slot.theme(), slot.time(), slot.date()));
    }

    private Slot saveSlot() {
        return saveSlot(LocalDate.of(2030, 5, 1));
    }

    private Slot saveSlot(LocalDate date) {
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        return new Slot(theme, time, date);
    }

    private Member saveMember(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }

    private record Slot(Theme theme, ReservationTime time, LocalDate date) {
    }
}

package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;
import roomescape.dto.MyReservationResponse;
import roomescape.dto.ReservationStatus;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;
import roomescape.service.MyReservationService;
import roomescape.service.ReservationPromotionService;
import roomescape.service.ReservationService;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ReservationCancelRollbackIntegrationTest {

    @MockitoBean
    private ReservationPromotionService reservationPromotionService;

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
    @DisplayName("대기 승격 실패 시 예약 삭제와 대기 삭제를 모두 롤백한다")
    void rollbackReservationAndWaitingDeletionWhenPromotionFails() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = saveTheme();
        ReservationTime time = saveTime();
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.saveAndFlush(new Reservation(brown, theme, time, date));
        Waiting waiting = waitingRepository.saveAndFlush(new Waiting(
                cony,
                theme,
                time,
                date,
                LocalDateTime.of(2030, 1, 1, 10, 0)
        ));
        willThrow(new RuntimeException("promotion failed"))
                .given(reservationPromotionService)
                .promote(any(Waiting.class));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("promotion failed");

        assertThat(reservationRepository.existsById(reservation.getId())).isTrue();
        assertThat(waitingRepository.existsById(waiting.getId())).isTrue();

        List<MyReservationResponse> brownMine = myReservationService.findMine(loginMember(brown));
        assertThat(brownMine).singleElement().satisfies(response ->
                assertThat(response.status()).isEqualTo(ReservationStatus.RESERVED));

        List<MyReservationResponse> conyMine = myReservationService.findMine(loginMember(cony));
        assertThat(conyMine).singleElement().satisfies(response -> {
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

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

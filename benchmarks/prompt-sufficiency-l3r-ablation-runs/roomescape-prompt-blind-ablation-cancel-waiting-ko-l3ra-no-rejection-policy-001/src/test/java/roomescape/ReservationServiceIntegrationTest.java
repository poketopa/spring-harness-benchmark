package roomescape;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.dto.WaitingRequest;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.ThemeRepository;
import roomescape.repository.WaitingRepository;
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
    private MemberRepository memberRepository;

    @MockitoSpyBean
    private ReservationRepository reservationRepository;

    @Autowired
    private WaitingRepository waitingRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Test
    @DisplayName("예약 취소 중 승격 예약 저장에 실패하면 예약 삭제와 대기 삭제가 롤백된다")
    void cancelReservationRollsBackWhenPromotionFails() {
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Theme theme = themeRepository.save(new Theme("어둠의 방", "방탈출", "https://example.com/dark.jpg"));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));
        waitingService.create(loginMember(cony), new WaitingRequest(date, time.getId(), theme.getId()));
        doThrow(new DataIntegrityViolationException("promotion failed"))
                .when(reservationRepository)
                .saveAndFlush(argThat(candidate -> candidate.getMember().getId().equals(cony.getId())));

        assertThatThrownBy(() -> reservationService.cancel(loginMember(brown), reservation.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(reservationRepository.findByThemeAndTimeAndDate(theme, time, date))
                .isPresent()
                .get()
                .extracting(savedReservation -> savedReservation.getMember().getId())
                .isEqualTo(brown.getId());
        assertThat(waitingRepository.findAll()).hasSize(1);
    }

    private Member saveMember(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

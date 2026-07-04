package roomescape.service;

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
import org.springframework.transaction.annotation.Transactional;
import roomescape.auth.LoginMember;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.dto.ReservationChangeRequest;
import roomescape.dto.ReservationResponse;
import roomescape.exception.ErrorCode;
import roomescape.exception.RoomescapeException;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Test
    @DisplayName("매니저는 자기 매장 예약만 조회한다")
    void managerFindsOnlyOwnStoreReservations() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member otherManager = saveManager("다른매니저", "other-manager@example.com");
        Member brown = saveMember("브라운", "brown@example.com");
        Member cony = saveMember("코니", "cony@example.com");
        Store store = saveStore("강남점", manager);
        Store otherStore = saveStore("잠실점", otherManager);
        Theme theme = saveTheme("어둠의 방", store);
        Theme otherTheme = saveTheme("빛의 방", otherStore);
        ReservationTime time = saveTime(LocalTime.of(10, 0));
        LocalDate date = LocalDate.of(2030, 5, 1);
        Reservation reservation = reservationRepository.save(new Reservation(brown, theme, time, date));
        reservationRepository.save(new Reservation(cony, otherTheme, time, date));

        List<ReservationResponse> responses = reservationService.findManaged(loginMember(manager));

        assertThat(responses).extracting(ReservationResponse::id).containsExactly(reservation.getId());
    }

    @Test
    @DisplayName("매니저는 자기 매장 예약을 변경할 수 있다")
    void managerChangesOwnStoreReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member brown = saveMember("브라운", "brown@example.com");
        Store store = saveStore("강남점", manager);
        Theme theme = saveTheme("어둠의 방", store);
        ReservationTime tenOClock = saveTime(LocalTime.of(10, 0));
        ReservationTime elevenOClock = saveTime(LocalTime.of(11, 0));
        Reservation reservation = reservationRepository.save(new Reservation(
                brown,
                theme,
                tenOClock,
                LocalDate.of(2030, 5, 1)
        ));

        ReservationResponse response = reservationService.changeManaged(
                loginMember(manager),
                reservation.getId(),
                new ReservationChangeRequest(LocalDate.of(2030, 5, 2), elevenOClock.getId())
        );

        assertThat(response.date()).isEqualTo(LocalDate.of(2030, 5, 2));
        assertThat(response.timeId()).isEqualTo(elevenOClock.getId());
    }

    @Test
    @DisplayName("매니저가 다른 매장 예약을 변경하면 인가 예외가 발생한다")
    void managerChangingOtherStoreReservationThrowsForbidden() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member otherManager = saveManager("다른매니저", "other-manager@example.com");
        Member brown = saveMember("브라운", "brown@example.com");
        saveStore("강남점", manager);
        Store otherStore = saveStore("잠실점", otherManager);
        Theme otherTheme = saveTheme("빛의 방", otherStore);
        ReservationTime time = saveTime(LocalTime.of(10, 0));
        Reservation reservation = reservationRepository.save(new Reservation(
                brown,
                otherTheme,
                time,
                LocalDate.of(2030, 5, 1)
        ));

        assertThatThrownBy(() -> reservationService.changeManaged(
                loginMember(manager),
                reservation.getId(),
                new ReservationChangeRequest(LocalDate.of(2030, 5, 2), time.getId())
        ))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }

    private Member saveManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }

    private Member saveMember(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private Store saveStore(String name, Member manager) {
        return storeRepository.save(new Store(name, manager));
    }

    private Theme saveTheme(String name, Store store) {
        return themeRepository.save(new Theme(name, "방탈출", "https://example.com/" + name + ".jpg", store));
    }

    private ReservationTime saveTime(LocalTime startAt) {
        return timeRepository.save(new ReservationTime(startAt));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

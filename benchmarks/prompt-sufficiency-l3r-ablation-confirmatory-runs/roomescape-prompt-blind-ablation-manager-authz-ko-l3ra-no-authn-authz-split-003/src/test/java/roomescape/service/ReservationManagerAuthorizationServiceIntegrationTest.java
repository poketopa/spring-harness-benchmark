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
class ReservationManagerAuthorizationServiceIntegrationTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ReservationTimeRepository timeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약만 조회할 수 있다")
    void managerFindsOnlyOwnStoreReservations() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member otherManager = saveManager("다른 매니저", "other-manager@example.com");
        Member customer = saveUser("브라운", "brown@example.com");
        Reservation ownStoreReservation = saveReservation(customer, saveStoreTheme("내 매장", manager), LocalTime.of(10, 0));
        saveReservation(customer, saveStoreTheme("다른 매장", otherManager), LocalTime.of(11, 0));

        List<ReservationResponse> responses = reservationService.findManaged(loginMember(manager));

        assertThat(responses)
                .extracting(ReservationResponse::id)
                .containsExactly(ownStoreReservation.getId());
    }

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 변경할 수 있다")
    void managerChangesOwnStoreReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member customer = saveUser("브라운", "brown@example.com");
        Reservation reservation = saveReservation(customer, saveStoreTheme("내 매장", manager), LocalTime.of(10, 0));
        ReservationTime changedTime = timeRepository.save(new ReservationTime(LocalTime.of(12, 0)));
        LocalDate changedDate = LocalDate.of(2030, 5, 2);

        ReservationResponse response = reservationService.changeManaged(
                loginMember(manager),
                reservation.getId(),
                new ReservationChangeRequest(changedDate, changedTime.getId())
        );

        assertThat(response.date()).isEqualTo(changedDate);
        assertThat(response.timeId()).isEqualTo(changedTime.getId());
    }

    @Test
    @DisplayName("매니저는 자신이 관리하지 않는 매장의 예약을 변경할 수 없다")
    void managerCannotChangeOtherStoreReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member otherManager = saveManager("다른 매니저", "other-manager@example.com");
        Member customer = saveUser("브라운", "brown@example.com");
        Reservation otherStoreReservation = saveReservation(customer, saveStoreTheme("다른 매장", otherManager), LocalTime.of(10, 0));
        ReservationTime changedTime = timeRepository.save(new ReservationTime(LocalTime.of(12, 0)));

        assertThatThrownBy(() -> reservationService.changeManaged(
                loginMember(manager),
                otherStoreReservation.getId(),
                new ReservationChangeRequest(LocalDate.of(2030, 5, 2), changedTime.getId())
        )).isInstanceOfSatisfying(RoomescapeException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("매니저가 아닌 사용자는 매장 예약을 조회할 수 없다")
    void nonManagerCannotFindManagedReservations() {
        Member user = saveUser("브라운", "brown@example.com");

        assertThatThrownBy(() -> reservationService.findManaged(loginMember(user)))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 삭제할 수 있다")
    void managerCancelsOwnStoreReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member customer = saveUser("브라운", "brown@example.com");
        Reservation reservation = saveReservation(customer, saveStoreTheme("내 매장", manager), LocalTime.of(10, 0));

        reservationService.cancelManaged(loginMember(manager), reservation.getId());

        assertThat(reservationRepository.findById(reservation.getId())).isEmpty();
    }

    @Test
    @DisplayName("매니저는 자신이 관리하지 않는 매장의 예약을 삭제할 수 없다")
    void managerCannotCancelOtherStoreReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member otherManager = saveManager("다른 매니저", "other-manager@example.com");
        Member customer = saveUser("브라운", "brown@example.com");
        Reservation reservation = saveReservation(customer, saveStoreTheme("다른 매장", otherManager), LocalTime.of(10, 0));

        assertThatThrownBy(() -> reservationService.cancelManaged(loginMember(manager), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("매니저가 아닌 사용자는 매장 예약을 삭제할 수 없다")
    void nonManagerCannotCancelManagedReservation() {
        Member manager = saveManager("매니저", "manager@example.com");
        Member user = saveUser("브라운", "brown@example.com");
        Reservation reservation = saveReservation(user, saveStoreTheme("내 매장", manager), LocalTime.of(10, 0));

        assertThatThrownBy(() -> reservationService.cancelManaged(loginMember(user), reservation.getId()))
                .isInstanceOfSatisfying(RoomescapeException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    private Member saveManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }

    private Member saveUser(String name, String email) {
        return memberRepository.save(new Member(name, email, "password"));
    }

    private Theme saveStoreTheme(String storeName, Member manager) {
        Store store = storeRepository.save(new Store(storeName, manager));
        return themeRepository.save(new Theme(storeName + " 테마", "방탈출", "https://example.com/theme.jpg", store));
    }

    private Reservation saveReservation(Member customer, Theme theme, LocalTime startAt) {
        ReservationTime time = timeRepository.save(new ReservationTime(startAt));
        return reservationRepository.save(new Reservation(customer, theme, time, LocalDate.of(2030, 5, 1)));
    }

    private LoginMember loginMember(Member member) {
        return new LoginMember(member.getId(), member.getName());
    }
}

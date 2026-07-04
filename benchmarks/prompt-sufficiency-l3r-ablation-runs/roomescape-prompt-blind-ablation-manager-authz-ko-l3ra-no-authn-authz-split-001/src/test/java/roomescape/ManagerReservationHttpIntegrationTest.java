package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.auth.AuthTokenProvider;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Role;
import roomescape.domain.Store;
import roomescape.domain.Theme;
import roomescape.repository.MemberRepository;
import roomescape.repository.ReservationRepository;
import roomescape.repository.ReservationTimeRepository;
import roomescape.repository.StoreRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationHttpIntegrationTest extends AcceptanceTestSupport {

    @Autowired
    private AuthTokenProvider authTokenProvider;

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
    @DisplayName("매니저는 자신이 관리하는 매장 예약만 조회한다")
    void managerFindsOwnStoreReservations() {
        Member manager = saveManager("매니저", "manager@example.com");
        Reservation ownStoreReservation = saveReservation(manager, "강남점", "어둠의 방");
        saveReservation(saveManager("다른 매니저", "other-manager@example.com"), "홍대점", "밝음의 방");

        ResponseEntity<List> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(authTokenProvider.createToken(manager))),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(body -> ((Number) ((Map<?, ?>) body).get("id")).longValue())
                .containsExactly(ownStoreReservation.getId());
    }

    @Test
    @DisplayName("매니저가 다른 매장 예약을 변경하면 찾을 수 없음 응답을 반환한다")
    void managerChangingOtherStoreReservationReturnsNotFound() {
        Member manager = saveManager("매니저", "manager@example.com");
        Reservation otherStoreReservation = saveReservation(
                saveManager("다른 매니저", "other-manager@example.com"),
                "홍대점",
                "밝음의 방"
        );
        ReservationTime changedTime = timeRepository.save(new ReservationTime(LocalTime.of(11, 0)));
        Map<String, Object> request = Map.of(
                "date", LocalDate.of(2030, 5, 2).toString(),
                "timeId", changedTime.getId()
        );

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations/" + otherStoreReservation.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders(authTokenProvider.createToken(manager))),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("code")).isEqualTo("RESERVATION_NOT_FOUND");
    }

    @Test
    @DisplayName("매니저가 아닌 회원이 매장 예약을 조회하면 인증 실패 응답을 반환한다")
    void nonManagerFindingManagedReservationsReturnsUnauthorized() {
        Member member = memberRepository.save(new Member("브라운", "brown@example.com", "password"));

        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(authTokenProvider.createToken(member))),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    private Reservation saveReservation(Member manager, String storeName, String themeName) {
        Member member = memberRepository.save(new Member(themeName + " 예약자", themeName + "@example.com", "password"));
        Store store = storeRepository.save(new Store(storeName, manager));
        Theme theme = themeRepository.save(new Theme(store, themeName, "방탈출", "https://example.com/" + themeName));
        ReservationTime time = timeRepository.save(new ReservationTime(LocalTime.of(10, 0)));
        return reservationRepository.saveAndFlush(new Reservation(member, theme, time, LocalDate.of(2030, 5, 1)));
    }

    private Member saveManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }
}

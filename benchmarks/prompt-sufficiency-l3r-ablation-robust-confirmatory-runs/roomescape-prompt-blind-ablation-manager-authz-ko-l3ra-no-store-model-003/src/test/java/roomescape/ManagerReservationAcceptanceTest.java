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
import roomescape.domain.ManagedStore;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.repository.ManagedStoreRepository;
import roomescape.repository.MemberRepository;
import roomescape.repository.ThemeRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ManagerReservationAcceptanceTest extends AcceptanceTestSupport {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ManagedStoreRepository managedStoreRepository;

    @Test
    @DisplayName("매니저는 자신이 관리하는 매장의 예약을 조회하고 변경하고 삭제할 수 있다")
    void managerFindsChangesAndCancelsOwnStoreReservation() {
        // given
        Member manager = createManager("매니저", "manager@example.com");
        String managerToken = login("manager@example.com");
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long themeId = createTheme("어둠의 방");
        long tenOClockId = createTime(LocalTime.of(10, 0));
        long elevenOClockId = createTime(LocalTime.of(11, 0));
        LocalDate originalDate = LocalDate.of(2030, 5, 1);
        LocalDate changedDate = LocalDate.of(2030, 5, 2);
        long reservationId = createReservationId(memberToken, originalDate, tenOClockId, themeId);
        manageStore(manager, themeId);

        // when
        ResponseEntity<List> found = findManagerReservations(managerToken);
        ResponseEntity<Map> changed = changeManagerReservation(
                managerToken,
                reservationId,
                changedDate,
                elevenOClockId
        );
        ResponseEntity<Void> canceled = cancelManagerReservation(managerToken, reservationId);

        // then
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(found.getBody()).hasSize(1);
        assertThat(((Map<?, ?>) found.getBody().getFirst()).get("id")).isEqualTo((int) reservationId);

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(changed.getBody().get("date")).isEqualTo(changedDate.toString());
        assertThat(changed.getBody().get("timeId")).isEqualTo((int) elevenOClockId);

        assertThat(canceled.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(findMine(memberToken)).isEmpty();
    }

    @Test
    @DisplayName("매니저는 다른 매장의 예약을 조회하거나 변경하거나 삭제할 수 없다")
    void managerCannotAccessOtherStoreReservation() {
        // given
        Member manager = createManager("매니저", "manager@example.com");
        String managerToken = login("manager@example.com");
        createMember("브라운", "brown@example.com");
        String memberToken = login("brown@example.com");
        long managedThemeId = createTheme("관리 매장");
        long otherThemeId = createTheme("다른 매장");
        long timeId = createTime(LocalTime.of(10, 0));
        long changedTimeId = createTime(LocalTime.of(11, 0));
        long reservationId = createReservationId(memberToken, LocalDate.of(2030, 5, 1), timeId, otherThemeId);
        manageStore(manager, managedThemeId);

        // when
        ResponseEntity<Map> found = findManagerReservation(managerToken, reservationId);
        ResponseEntity<Map> changed = changeManagerReservation(
                managerToken,
                reservationId,
                LocalDate.of(2030, 5, 2),
                changedTimeId
        );
        ResponseEntity<Void> canceled = cancelManagerReservation(managerToken, reservationId);

        // then
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(found.getBody().get("code")).isEqualTo("FORBIDDEN");

        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(changed.getBody().get("code")).isEqualTo("FORBIDDEN");

        assertThat(canceled.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("매니저가 아닌 로그인 사용자는 매니저 예약 조회 권한이 없다")
    void nonManagerCannotFindManagerReservations() {
        // given
        createMember("브라운", "brown@example.com");
        String userToken = login("brown@example.com");

        // when
        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)),
                Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().get("code")).isEqualTo("FORBIDDEN");
    }

    @Test
    @DisplayName("비로그인 사용자는 매니저 예약 조회 인증에 실패한다")
    void unauthenticatedCannotFindManagerReservations() {
        // when
        ResponseEntity<Map> response = restTemplate.exchange(
                "/manager/reservations",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                Map.class
        );

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().get("code")).isEqualTo("UNAUTHORIZED");
    }

    private Member createManager(String name, String email) {
        return memberRepository.save(new Member(name, email, "password", Role.MANAGER));
    }

    private void manageStore(Member manager, long themeId) {
        managedStoreRepository.save(new ManagedStore(manager, themeRepository.getByIdOrThrow(themeId)));
    }

    private long createReservationId(String token, LocalDate date, long timeId, long themeId) {
        return ((Number) createReservation(token, date, timeId, themeId)
                .getBody()
                .get("id")).longValue();
    }
}

package roomescape;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import roomescape.domain.Member;
import roomescape.domain.Role;
import roomescape.repository.MemberRepository;

abstract class AcceptanceTestSupport {

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private MemberRepository memberRepository;

    protected long createMember(String name, String email) {
        Map<String, Object> request = Map.of(
                "name", name,
                "email", email,
                "password", "password"
        );
        ResponseEntity<Map> response = restTemplate.postForEntity("/members", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) response.getBody().get("id")).longValue();
    }

    protected long createMember(String name, String email, Role role) {
        Member member = memberRepository.save(new Member(name, email, "password", role));
        return member.getId();
    }

    protected String login(String email) {
        Map<String, Object> request = Map.of(
                "email", email,
                "password", "password"
        );
        ResponseEntity<Map> response = restTemplate.postForEntity("/login", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) response.getBody().get("accessToken");
    }

    protected long createTheme(String name) {
        Map<String, Object> request = Map.of(
                "name", name,
                "description", "방탈출",
                "thumbnailUrl", "https://example.com/" + name + ".jpg"
        );
        ResponseEntity<Map> response = restTemplate.postForEntity("/admin/themes", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) response.getBody().get("id")).longValue();
    }

    protected long createTime(LocalTime startAt) {
        Map<String, Object> request = Map.of("startAt", startAt.toString());
        ResponseEntity<Map> response = restTemplate.postForEntity("/admin/times", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) response.getBody().get("id")).longValue();
    }

    protected ResponseEntity<Map> createReservation(String token, LocalDate date, long timeId, long themeId) {
        Map<String, Object> request = Map.of(
                "date", date.toString(),
                "timeId", timeId,
                "themeId", themeId
        );

        return restTemplate.exchange(
                "/reservations",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)),
                Map.class
        );
    }

    protected ResponseEntity<Map> createWaiting(String token, LocalDate date, long timeId, long themeId) {
        Map<String, Object> request = Map.of(
                "date", date.toString(),
                "timeId", timeId,
                "themeId", themeId
        );

        return restTemplate.exchange(
                "/waitings",
                HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(token)),
                Map.class
        );
    }

    protected ResponseEntity<Map> changeReservation(String token, long reservationId, LocalDate date, long timeId) {
        Map<String, Object> request = Map.of(
                "date", date.toString(),
                "timeId", timeId
        );

        return restTemplate.exchange(
                "/reservations/" + reservationId,
                HttpMethod.PUT,
                new HttpEntity<>(request, authHeaders(token)),
                Map.class
        );
    }

    protected ResponseEntity<Void> cancelReservation(String token, long reservationId) {
        return restTemplate.exchange(
                "/reservations/" + reservationId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }

    protected ResponseEntity<Void> cancelWaiting(String token, long waitingId) {
        return restTemplate.exchange(
                "/waitings/" + waitingId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Void.class
        );
    }

    protected ResponseEntity<Void> deleteTime(long timeId) {
        return restTemplate.exchange(
                "/admin/times/" + timeId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                Void.class
        );
    }

    protected List<Map<String, Object>> findThemeTimes(long themeId, LocalDate date) {
        ResponseEntity<List> response = restTemplate.getForEntity(
                "/themes/" + themeId + "/times?date=" + date,
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected List<Map<String, Object>> findMine(String token) {
        ResponseEntity<List> response = restTemplate.exchange(
                "/reservations/mine",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected List<Map<String, Object>> findAdminReservations(String token) {
        ResponseEntity<List> response = restTemplate.exchange(
                "/admin/reservations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                List.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }
}

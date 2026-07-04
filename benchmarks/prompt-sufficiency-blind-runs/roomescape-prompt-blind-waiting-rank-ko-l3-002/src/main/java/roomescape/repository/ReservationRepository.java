package roomescape.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationStatus;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByThemeAndTimeAndDate(Theme theme, ReservationTime time, LocalDate date);

    boolean existsByThemeAndTimeAndDateAndStatus(
            Theme theme,
            ReservationTime time,
            LocalDate date,
            ReservationStatus status
    );

    boolean existsByMemberAndThemeAndTimeAndDate(Member member, Theme theme, ReservationTime time, LocalDate date);

    Optional<Reservation> findByThemeAndTimeAndDate(Theme theme, ReservationTime time, LocalDate date);

    @EntityGraph(attributePaths = {"theme", "time"})
    List<Reservation> findAllByMemberOrderByDateAscTimeStartAtAsc(Member member);

    List<Reservation> findAllByThemeAndDate(Theme theme, LocalDate date);

    int countByThemeAndTimeAndDateAndStatus(Theme theme, ReservationTime time, LocalDate date, ReservationStatus status);

    int countByThemeAndTimeAndDateAndStatusAndIdLessThanEqual(
            Theme theme,
            ReservationTime time,
            LocalDate date,
            ReservationStatus status,
            Long id
    );
}

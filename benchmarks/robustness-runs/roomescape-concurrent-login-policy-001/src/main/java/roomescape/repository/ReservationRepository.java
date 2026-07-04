package roomescape.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Member;
import roomescape.domain.Reservation;
import roomescape.domain.ReservationTime;
import roomescape.domain.Store;
import roomescape.domain.Theme;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByThemeAndTimeAndDate(Theme theme, ReservationTime time, LocalDate date);

    boolean existsByThemeAndTimeAndDateAndIdNot(Theme theme, ReservationTime time, LocalDate date, Long id);

    boolean existsByTime(ReservationTime time);

    Optional<Reservation> findByThemeAndTimeAndDate(Theme theme, ReservationTime time, LocalDate date);

    @EntityGraph(attributePaths = {"theme", "time"})
    Optional<Reservation> findByIdAndMember(Long id, Member member);

    @Override
    @EntityGraph(attributePaths = {"member", "theme", "theme.store", "time"})
    Optional<Reservation> findById(Long id);

    @EntityGraph(attributePaths = {"member", "theme", "time"})
    List<Reservation> findAllByMemberOrderByDateAscTimeStartAtAsc(Member member);

    @EntityGraph(attributePaths = {"member", "theme", "time"})
    List<Reservation> findAllByThemeStoreOrderByDateAscTimeStartAtAsc(Store store);

    @EntityGraph(attributePaths = {"time"})
    List<Reservation> findAllByThemeAndDate(Theme theme, LocalDate date);
}

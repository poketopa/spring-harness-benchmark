package roomescape.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Member;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {

    boolean existsByMemberAndThemeAndTimeAndDate(Member member, Theme theme, ReservationTime time, LocalDate date);

    long countByThemeAndTimeAndDate(Theme theme, ReservationTime time, LocalDate date);

    long countByThemeAndTimeAndDateAndIdLessThan(Theme theme, ReservationTime time, LocalDate date, Long id);

    Optional<Waiting> findByIdAndMember(Long id, Member member);

    @EntityGraph(attributePaths = {"theme", "time"})
    List<Waiting> findAllByMemberOrderByDateAscTimeStartAtAsc(Member member);
}

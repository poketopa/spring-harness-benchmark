package roomescape.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Member;
import roomescape.domain.ReservationTime;
import roomescape.domain.ReservationWaiting;
import roomescape.domain.Theme;

public interface ReservationWaitingRepository extends JpaRepository<ReservationWaiting, Long> {

    boolean existsByMemberAndThemeAndTimeAndDate(Member member, Theme theme, ReservationTime time, LocalDate date);

    @EntityGraph(attributePaths = {"member", "theme", "time"})
    List<ReservationWaiting> findAllByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(
            Theme theme,
            ReservationTime time,
            LocalDate date
    );

    @EntityGraph(attributePaths = {"theme", "time"})
    List<ReservationWaiting> findAllByMember(Member member);
}

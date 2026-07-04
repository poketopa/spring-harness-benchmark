package roomescape.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import roomescape.domain.Member;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {

    boolean existsByMemberAndThemeAndTimeAndDate(
            Member member,
            Theme theme,
            ReservationTime time,
            LocalDate date
    );

    @EntityGraph(attributePaths = {"theme", "time", "member"})
    List<Waiting> findAllByMember(Member member);

    @EntityGraph(attributePaths = {"theme", "time", "member"})
    List<Waiting> findAllByThemeAndTimeAndDateOrderByCreatedAtAscIdAsc(
            Theme theme,
            ReservationTime time,
            LocalDate date
    );
}

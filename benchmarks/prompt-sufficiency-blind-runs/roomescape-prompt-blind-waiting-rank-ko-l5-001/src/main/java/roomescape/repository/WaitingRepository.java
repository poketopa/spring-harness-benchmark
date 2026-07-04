package roomescape.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import roomescape.domain.Member;
import roomescape.domain.ReservationTime;
import roomescape.domain.Theme;
import roomescape.domain.Waiting;

public interface WaitingRepository extends JpaRepository<Waiting, Long> {

    boolean existsByMemberAndThemeAndTimeAndDate(Member member, Theme theme, ReservationTime time, LocalDate date);

    @Query("""
            select count(w)
            from Waiting w
            where w.theme = :theme
                and w.time = :time
                and w.date = :date
                and (
                    w.requestedAt < :requestedAt
                    or (w.requestedAt = :requestedAt and w.id <= :id)
                )
            """)
    int countRank(
            @Param("theme") Theme theme,
            @Param("time") ReservationTime time,
            @Param("date") LocalDate date,
            @Param("requestedAt") LocalDateTime requestedAt,
            @Param("id") Long id
    );

    @EntityGraph(attributePaths = {"theme", "time"})
    List<Waiting> findAllByMemberOrderByDateAscTimeStartAtAscRequestedAtAscIdAsc(Member member);
}

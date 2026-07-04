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
import roomescape.domain.ReservationWaiting;
import roomescape.domain.Theme;

public interface ReservationWaitingRepository extends JpaRepository<ReservationWaiting, Long> {

    boolean existsByMemberAndThemeAndTimeAndDate(Member member, Theme theme, ReservationTime time, LocalDate date);

    @EntityGraph(attributePaths = {"theme", "time"})
    List<ReservationWaiting> findAllByMemberOrderByDateAscTimeStartAtAscCreatedAtAscIdAsc(Member member);

    @Query("""
            select count(waiting)
            from ReservationWaiting waiting
            where waiting.theme = :theme
              and waiting.time = :time
              and waiting.date = :date
              and (
                    waiting.createdAt < :createdAt
                    or (waiting.createdAt = :createdAt and waiting.id < :id)
              )
            """)
    long countAhead(
            @Param("theme") Theme theme,
            @Param("time") ReservationTime time,
            @Param("date") LocalDate date,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id
    );
}

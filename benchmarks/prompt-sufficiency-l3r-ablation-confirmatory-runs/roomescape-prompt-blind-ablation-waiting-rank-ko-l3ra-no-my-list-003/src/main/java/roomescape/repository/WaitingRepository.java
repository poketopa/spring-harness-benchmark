package roomescape.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
                    w.createdAt < :createdAt
                    or (w.createdAt = :createdAt and w.id <= :id)
              )
            """)
    int countRankBySlotAndCreatedAtAndId(
            Theme theme,
            ReservationTime time,
            LocalDate date,
            LocalDateTime createdAt,
            Long id
    );
}

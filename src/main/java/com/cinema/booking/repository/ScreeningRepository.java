package com.cinema.booking.repository;

import com.cinema.booking.model.Screening;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    // pobiera nadchodzace seanse
    List<Screening> findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime now);

    // Zapytanie JPQL sprawdzajace czy w danej sali istnieja seanse nachodzace czasowo
    @Query(value = "SELECT COUNT(*) > 0 FROM screenings s " +
            "JOIN movies m ON s.movie_id = m.id " +
            "WHERE s.cinema_hall_id = :hallId " +
            "AND :newStart < (s.start_time + (m.duration_minutes * INTERVAL '1 minute')) " +
            "AND :newEnd > s.start_time",
            nativeQuery = true)
    boolean existsOverlappingScreening(@Param("hallId") Long hallId,
                                       @Param("newStart") LocalDateTime newStart,
                                       @Param("newEnd") LocalDateTime newEnd);
}

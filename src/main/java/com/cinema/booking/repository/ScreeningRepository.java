package com.cinema.booking.repository;

import com.cinema.booking.model.Screening;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    //downloads the screening and block its row in database (SELECT ... FOR UPDATE) until transaction is finished
    //thanks to that, two parallel reservations on the same screening are made one after second
    //so the second one will see that this place is already taken - because of that we won't have 2 reservations for one seat
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Screening s WHERE s.id = :id")
    Optional<Screening> findByIdForUpdate(@Param("id") Long id);

    //1. download all upcoming screenings (movie and hall are downloaded instantly with one query
    @EntityGraph(attributePaths = {"movie", "cinemaHall"})
    List<Screening> findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime now);

    //2. download screenings starting at specific time range
    @EntityGraph(attributePaths = {"movie", "cinemaHall"})
    List<Screening> findByStartTimeBetweenOrderByStartTimeAsc(LocalDateTime start, LocalDateTime end);

    //3. native SQL query (PostgreSQL) checking if there are overlapping (by time) screenings in specific hall
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

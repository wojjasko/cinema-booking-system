package com.cinema.booking.repository;

import com.cinema.booking.model.Seat;
import com.cinema.booking.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    //pobiera wszystkie bilety sprzedane na dany seans
    List<Ticket> findByReservationScreeningId(Long screeningId);

    @Query(value = "SELECT s.* FROM seats s " +
            "JOIN tickets t ON s.id = t.seat_id " +
            "JOIN reservations r ON t.reservation_id = r.id " +
            "WHERE r.screening_id = :screeningId " +
            "AND r.status IN ('PENDING', 'PAID')",
            nativeQuery = true)
    List<Seat> findReservedSeatsByScreeningId(@Param("screeningId") Long screeningId);
}

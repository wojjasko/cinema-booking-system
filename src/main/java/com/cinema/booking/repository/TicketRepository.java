package com.cinema.booking.repository;

import com.cinema.booking.model.Seat;
import com.cinema.booking.model.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    //download all tickets sold for specific screening (together with reservation and seat)
    @EntityGraph(attributePaths = {"reservation", "seat"})
    List<Ticket> findByReservationScreeningId(Long screeningId);

    //returns only ID from taken seats
    @Query("SELECT t.seat.id FROM Ticket t " +
            "WHERE t.reservation.screening.id = :screeningId " +
            "AND t.reservation.status IN (" +
            "com.cinema.booking.model.ReservationStatus.PENDING, " +
            "com.cinema.booking.model.ReservationStatus.PAID)")
    List<Long> findOccupiedSeatIdsByScreeningId(@Param("screeningId") Long screeningId);
}

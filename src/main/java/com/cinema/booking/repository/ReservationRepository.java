package com.cinema.booking.repository;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>{

    //download all reservations made by specific user (with the data needed in "My Reservations view")
    @EntityGraph(attributePaths = {"screening.movie", "screening.cinemaHall", "tickets.seat"})
    List<Reservation> findByCustomerEmailOrderByIdDesc(String customerEmail);

    //download reservations from specific user made on specific screening
    List<Reservation> findByCustomerEmailAndScreeningId(String customerEmail, Long screeningId);

    //finds reservations with specific status made before indicated time
    List<Reservation> findByStatusAndReservationTimeBefore(ReservationStatus status, LocalDateTime cutOffTime);
}

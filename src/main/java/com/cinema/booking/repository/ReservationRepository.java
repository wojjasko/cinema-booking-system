package com.cinema.booking.repository;

import com.cinema.booking.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long>{

    //pobiera wszystkie rezerwacje zlozone przez konkretnego usera
    //SELECT * FROM reservations WHERE customer_email = ? ORDER BY id DESC
    List<Reservation> findByCustomerEmailOrderByIdDesc(String customerEmail);
}
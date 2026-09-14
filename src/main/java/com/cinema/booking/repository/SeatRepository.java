package com.cinema.booking.repository;

import com.cinema.booking.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {
    //JPA generuje zapytanie "znajdz wszystkie miejsca gdzie cinemaHall.id = jakis tam argument i posotruj rosnaco wg rzedu a potem wg numeru
    List<Seat> findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(Long cinemaHallId);
}

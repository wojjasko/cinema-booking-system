package com.cinema.booking.integration;

import com.cinema.booking.model.CinemaHall;
import com.cinema.booking.model.Movie;
import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.Screening;
import com.cinema.booking.model.Seat;
import com.cinema.booking.repository.CinemaHallRepository;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.ReservationRepository;
import com.cinema.booking.repository.ScreeningRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.repository.TicketRepository;
import com.cinema.booking.service.ReservationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// shared base for integration tests; runs the whole spring application with REAL PostgreSQL
// (profile "test" -> cinemadb_test database) and before each test it creates its own data: movie, hall with 4 seats
// and screening
// after the test, everything is cleaned, so tests are not colliding with each other

@SpringBootTest
@ActiveProfiles("test")
abstract class IntegrationTestBase {

    protected static final String OWNER = "wlasciciel@test.pl";

    @Autowired protected MovieRepository movieRepository;
    @Autowired protected CinemaHallRepository cinemaHallRepository;
    @Autowired protected SeatRepository seatRepository;
    @Autowired protected ScreeningRepository screeningRepository;
    @Autowired protected ReservationRepository reservationRepository;
    @Autowired protected TicketRepository ticketRepository;
    @Autowired protected ReservationService reservationService;

    protected Movie movie;
    protected CinemaHall hall;
    protected Screening screening;
    protected List<Seat> seats;

    // ID of reservations created in test - this need to be claned after the test (the list is safe for many threads)
    protected final List<Long> reservationIds = new CopyOnWriteArrayList<>();

    @BeforeEach
    void createFixture() {
        movie = movieRepository.save(Movie.builder()
                .title("Film testowy")
                .description("Dane do testow integracyjnych")
                .durationMinutes(100)
                .genre("Test")
                .build());

        hall = cinemaHallRepository.save(CinemaHall.builder()
                .name("Sala testowa")
                .totalRows(1)
                .seatsPerRow(4)
                .build());

        List<Seat> newSeats = new ArrayList<>();
        for (int seatNumber = 1; seatNumber <= 4; seatNumber++) {
            newSeats.add(Seat.builder().rowNumber(1).seatNumber(seatNumber).cinemaHall(hall).build());
        }
        seats = seatRepository.saveAll(newSeats);

        screening = screeningRepository.save(Screening.builder()
                .movie(movie)
                .cinemaHall(hall)
                .startTime(LocalDateTime.now().plusDays(1))
                .price(new BigDecimal("25.00"))
                .build());
    }

    @AfterEach
    void cleanUpFixture() {
        // the order matters here (foreign keys): reservations (with tickets) -> screening -> seat -> hall -> movie
        reservationRepository.deleteAllById(reservationIds);
        reservationIds.clear();
        screeningRepository.delete(screening);
        seatRepository.deleteAll(seats);
        cinemaHallRepository.delete(hall);
        movieRepository.delete(movie);
    }

    // creates new reservation for one place and save it for later cleaning
    protected Reservation reserve(Seat seat, String customerEmail) {
        Reservation reservation = reservationService.createReservation(
                screening.getId(), List.of(seat.getId()), customerEmail);
        reservationIds.add(reservation.getId());
        return reservation;
    }
}

package com.cinema.booking.service;

import com.cinema.booking.dto.SeatOccupancyDto;
import com.cinema.booking.model.*;
import com.cinema.booking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final ScreeningService screeningService;

    //returns the set of seats ID, that are taken for specific screening
    public Set<Long> getOccupiedSeatIds(Long screeningId) {
        return new HashSet<>(ticketRepository.findOccupiedSeatIdsByScreeningId(screeningId));
    }

    //map: seat ID -> reservation data (used in hall view in admin panel)
    @Transactional(readOnly = true)
    public Map<Long, SeatOccupancyDto> getSeatOccupancyBySeatId(Long screeningId) {
        return ticketRepository.findByReservationScreeningId(screeningId).stream()
                .collect(Collectors.toMap(
                        ticket -> ticket.getSeat().getId(),
                        ticket -> new SeatOccupancyDto(
                                ticket.getId(),
                                ticket.getReservation().getCustomerEmail(),
                                ticket.getReservation().getReservationTime()),
                        (first, second) -> first
                ));
    }

    //creating a new reservation in database
    @Transactional
    public Reservation createReservation(Long screeningId, List<Long> seatIds, String customerEmail) {
        //1. getting screening from the database
        Screening screening = screeningService.getScreeningById(screeningId);

        //2. getting seats from the database and checking if they all exist and belongs to the hall
        Set<Long> requestedSeatIds = new HashSet<>(seatIds);
        List<Seat> selectedSeats = seatRepository.findAllById(requestedSeatIds);

        if (selectedSeats.size() != requestedSeatIds.size()) {
            throw new IllegalArgumentException("Wybrano nieistniejące miejsce.");
        }

        Long hallId = screening.getCinemaHall().getId();
        for (Seat seat : selectedSeats) {
            if (!seat.getCinemaHall().getId().equals(hallId)) {
                throw new IllegalArgumentException("Wybrane miejsce nie należy do sali tego seansu.");
            }
        }

        //3. checking if some seat was not already taken
        Set<Long> occupiedSeatIds = getOccupiedSeatIds(screeningId);
        for (Seat seat : selectedSeats) {
            if (occupiedSeatIds.contains(seat.getId())) {
                throw new IllegalStateException("Miejsce o numerze " + seat.getSeatNumber() + " w rzędzie " + seat.getRowNumber() + " jest już zajęte.");
            }
        }

        //4. counting the total price (amount of tickets * ticket price)
        BigDecimal totalPrice = screening.getPrice().multiply(BigDecimal.valueOf(selectedSeats.size()));

        //5. creating the new reservation object (with PENDING status)
        Reservation reservation = Reservation.builder()
                .screening(screening)
                .customerEmail(customerEmail)
                .reservationTime(LocalDateTime.now())
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING)
                .tickets(new ArrayList<>())
                .build();

        //6. creating tickets for chosen seats
        for (Seat seat : selectedSeats) {
            reservation.getTickets().add(Ticket.builder()
                    .reservation(reservation)
                    .seat(seat)
                    .build());
        }

        //7. saving the reservation (thanks to the CASCADE type all - the tickets will be saved too)
        return reservationRepository.save(reservation);
    }

    public Reservation getReservationById(Long reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono rezerwacji o id: " + reservationId));
    }

    public List<Reservation> getUserReservations(String customerEmail){
        return reservationRepository.findByCustomerEmailOrderByIdDesc(customerEmail);
    }

    @Transactional
    public void cancelSingleTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Bilet nie istnieje"));
        Reservation reservation = ticket.getReservation();

        // orphanRemoval = true - will remove the ticket from the database after removing him from reservation collection
        reservation.getTickets().remove(ticket);

        // if the reservation has no tickets - we remove it
        if (reservation.getTickets().isEmpty()) {
            reservationRepository.delete(reservation);
        }
    }

    @Transactional
    public void cancelAllUserTicketsForScreening(Long screeningId, String customerEmail) {
        // cascade = ALL on Reservation.tickets also removes the tickets
        reservationRepository.deleteAll(
                reservationRepository.findByCustomerEmailAndScreeningId(customerEmail, screeningId));
    }

    @Transactional
    public boolean processBlikPayment(Long reservationId, String blikCode){
        if (blikCode == null || !blikCode.matches("\\d{6}")){
            return false;
        }

        Reservation reservation = getReservationById(reservationId);

        // we can only pay for reservation with PENDING status
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Ta rezerwacja nie oczekuje już na płatność.");
        }

        reservation.setStatus(ReservationStatus.PAID);
        reservationRepository.save(reservation);
        return true;
    }
}

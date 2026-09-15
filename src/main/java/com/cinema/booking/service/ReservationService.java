package com.cinema.booking.service;

import com.cinema.booking.model.*;
import com.cinema.booking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TicketRepository ticketRepository;
    private final SeatRepository seatRepository;
    private final ScreeningService screeningService;

    //pobiera zestaw (set) ID foteli, tkore sa zajete dla danego seansu
    public Set<Long> getOccupiedSeatIds(Long screeningId) {
        List<Ticket> tickets = ticketRepository.findByReservationScreeningId(screeningId);

        return tickets.stream()
                .map(ticket -> ticket.getSeat().getId())
                .collect(Collectors.toSet());
    }

    //tworzenie nowej rezerwacji w bazie danych
    @Transactional
    public Reservation createReservation(Long screeningId, List<Long> seatIds, String customerEmail) {
        //1. pobieranie seansu z bazy
        Screening screening = screeningService.getScreeningById(screeningId);

        //2. pobieranie fotelow z bazy
        List<Seat> selectedSeats = seatRepository.findAllById(seatIds);

        //3. sprawdzanie czy ktorys fotel nie zostal juz wybrany
        Set<Long> occupiedSeatIds = getOccupiedSeatIds(screeningId);
        for (Seat seat : selectedSeats) {
            if (occupiedSeatIds.contains(seat.getId())) {
                throw new IllegalStateException("Miejsce o numerze " + seat.getSeatNumber() + " w rzędzie " + seat.getRowNumber() + " jest już zajęte.");
            }
        }

        //4. obliczanie lacznej ceny (liczba biletow * cena biletu za seans)
        BigDecimal totalPrice = screening.getPrice().multiply(BigDecimal.valueOf(selectedSeats.size()));

        //5. tworzenie nowego obiektu rezerwacji (ze statusem PENDING)
        Reservation reservation = Reservation.builder()
                .screening(screening)
                .customerEmail(customerEmail)
                .reservationTime(LocalDateTime.now())
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING)
                .tickets(new ArrayList<>())
                .build();

        //6. tworzenie biletow dla wybranych miejsc
        for (Seat seat : selectedSeats) {
            Ticket ticket = Ticket.builder()
                    .reservation(reservation)
                    .seat(seat)
                    .build();

            reservation.getTickets().add(ticket);
        }

        //7. zapisujemy rezerwacje (dzieki cascade type all zapisza sie tez bilety)
        return reservationRepository.save(reservation);
    }

    public List<Reservation> getUserReservations(String customerEmail){
        return reservationRepository.findByCustomerEmailOrderByIdDesc(customerEmail);
    }

    @Transactional
    public void cancelSingleTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Bilet nie istnieje"));
        Reservation reservation = ticket.getReservation();

        reservation.getTickets().remove(ticket);
        ticketRepository.delete(ticket);

        // jesli rezerwacja nie ma juz zadnych biletow, to ja usuwamy
        if (reservation.getTickets().isEmpty()) {
            reservationRepository.delete(reservation);
        }
    }

    @Transactional
    public void cancelAllUserTicketsForScreening(Long screeningId, String customerEmail) {
        List<Reservation> userReservations = reservationRepository.findByCustomerEmailOrderByIdDesc(customerEmail);
        for (Reservation res : userReservations) {
            if (res.getScreening().getId().equals(screeningId)) {
                ticketRepository.deleteAll(res.getTickets());
                reservationRepository.delete(res);
            }
        }
    }

    @Transactional
    public boolean processBlikPayment(Long reservationId, String blikCode){
        if (blikCode == null || !blikCode.matches("\\d{6}")){
            return false;
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono rezerwacji"));

        reservation.setStatus(ReservationStatus.PAID);
        reservationRepository.save(reservation);
        return true;
    }
}

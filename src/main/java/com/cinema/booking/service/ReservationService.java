package com.cinema.booking.service;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.Screening;
import com.cinema.booking.model.Seat;
import com.cinema.booking.model.Ticket;
import com.cinema.booking.repository.ReservationRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.repository.TicketRepository;
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
    public Set<Long> getOccupiedSeatIds(Long screeningId){
        List<Ticket> tickets = ticketRepository.findByReservationScreeningId(screeningId);

        return tickets.stream()
                .map(ticket -> ticket.getSeat().getId())
                .collect(Collectors.toSet());
    }

    //tworzenie nowej rezerwacji w bazie danych
    @Transactional
    public Reservation createReservation(Long screeningId, List<Long> seatIds, String customerEmail){
        //1. pobieranie seansu z bazy
        Screening screening = screeningService.getScreeningById(screeningId);

        //2. pobieranie wybranego fotelu z bazy
        List<Seat> selectedSeats = seatRepository.findAllById(seatIds);

        //3. sprawdzanie czy ktorys fotel nie zostal juz wybrany
        Set<Long> occupiedSeatIds = getOccupiedSeatIds(screeningId);
        for(Seat seat: selectedSeats){
            if(occupiedSeatIds.contains(seat.getId())){
                throw new RuntimeException("Miejsce o ID " + seat.getId() + " jest juz zajete.");
            }
        }

        //4. obliczanie lacznej ceny (liczba biletow * cena biletu za seans)
        BigDecimal totalPrice = screening.getPrice().multiply(new BigDecimal(selectedSeats.size()));

        //5. tworzenie obiektu rezerwacji
        Reservation reservation = Reservation.builder()
                .screening(screening)
                .customerEmail(customerEmail)
                .reservationTime(LocalDateTime.now())
                .totalPrice(totalPrice)
                .tickets(new ArrayList<>())
                .build();

        //6. tworzymy obiekty biletow i przypisujemy je do rezerwacji
        for(Seat seat: selectedSeats){
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

        // jesli rezerwacja nie ma juz zadnych biletow to ja usuwamy
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
}

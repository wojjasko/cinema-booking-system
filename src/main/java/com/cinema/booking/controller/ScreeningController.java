package com.cinema.booking.controller;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.Screening;
import com.cinema.booking.model.Seat;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.service.ReservationService;
import com.cinema.booking.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class ScreeningController {

    private final ScreeningService screeningService;
    private final SeatRepository seatRepository;
    private final ReservationService reservationService;

    //1. pobieranie widoku sali kinowej z miejscami
    @GetMapping("/screenings/{id}")
    public String showScreeningDetails(@PathVariable Long id, Model model) {
        Screening screening = screeningService.getScreeningById(id);

        List<Seat> seats = seatRepository.findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(
                screening.getCinemaHall().getId()
        );

        Set<Long> occupiedSeatIds = reservationService.getOccupiedSeatIds(id);

        model.addAttribute("screening", screening);
        model.addAttribute("seats", seats);
        model.addAttribute("occupiedSeatIds", occupiedSeatIds);

        return "screening-details";
    }

    // 2. osbluga wyslanego formularza rejestracji
    @PostMapping("/screenings/{id}/reserve")
    public String processReservation(@PathVariable Long id,
                                     @RequestParam List<Long> selectedSeatIds,
                                     Principal principal, //automatyczne pobieranie danych (emailu) uzytkownika
                                     Model model){

        //principal to standardowy interfejs javy reprezentujacy tozsamosc zalogowanej osoby
        String customerEmail = principal.getName();

        //wywolujemy serwis do stworzenia rezerwacji
        Reservation reservation = reservationService.createReservation(id, selectedSeatIds, customerEmail);

        //przekazujemy stworzona rezerwacje do widoku potwierdzenia
        model.addAttribute("reservation", reservation);

        return "reservation-confirmation";
    }
}
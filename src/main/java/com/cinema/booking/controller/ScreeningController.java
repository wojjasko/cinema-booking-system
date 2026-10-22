package com.cinema.booking.controller;

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

import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class ScreeningController {

    private final ScreeningService screeningService;
    private final SeatRepository seatRepository;
    private final ReservationService reservationService;

    //view of the cinema hall with seats (form register send the data to POST /reserve in ReservationController)
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
}

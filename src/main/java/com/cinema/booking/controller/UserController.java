package com.cinema.booking.controller;


import com.cinema.booking.model.Reservation;
import com.cinema.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final ReservationService reservationService;

    @GetMapping("/my-reservations")
    public String showUserReservations(Principal principal, Model model){
        //pobieranie emailu zalogowanego uzytkownika ze spring security
        String customerEmail = principal.getName();

        List<Reservation> userReservations = reservationService.getUserReservations(customerEmail);

        model.addAttribute("reservations",userReservations);
        return "my-reservations";
    }

}

package com.cinema.booking.controller;

import com.cinema.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final ReservationService reservationService;
}
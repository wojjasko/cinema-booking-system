package com.cinema.booking.controller;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.ReservationStatus;
import com.cinema.booking.service.PdfService;
import com.cinema.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final PdfService pdfService;

    // 1. form handling with chosen seats under /reserve
    @PostMapping("/reserve")
    public String createReservation(@RequestParam Long screeningId,
                                    @RequestParam(name = "seatIds", required = false) List<Long> seatIds,
                                    Principal principal) {
        if (seatIds == null || seatIds.isEmpty()) {
            return "redirect:/screenings/" + screeningId + "?error=no_seats";
        }

        Reservation reservation = reservationService.createReservation(screeningId, seatIds, principal.getName());

        //redirect to BLIK's payment page
        return "redirect:/payment/" + reservation.getId();
    }

    // 2. BLIK form view
    @GetMapping("/payment/{id}")
    public String showPaymentPage(@PathVariable Long id, Model model, Principal principal) {
        //only the owner of the reservation can see its payment page
        Reservation reservation = reservationService.getReservationForUser(id, principal.getName());

        //if the payment is paid or cancelled - we head back to the list
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            return "redirect:/my-reservations";
        }

        model.addAttribute("reservation", reservation);
        return "payment";
    }

    // 3. processing BLIK payment
    @PostMapping("/payment/{id}/pay")
    public String processPayment(@PathVariable Long id,
                                 @RequestParam String blikCode,
                                 Model model,
                                 Principal principal) {
        boolean success;
        try {
            success = reservationService.processBlikPayment(id, blikCode, principal.getName());
        } catch (IllegalStateException e) {
            //reservation is no longer waiting for payment (expired / cancelled / changed in the meantime)
            return "redirect:/my-reservations?paymentFailed=true";
        }

        if (!success) {
            model.addAttribute("reservation", reservationService.getReservationForUser(id, principal.getName()));
            model.addAttribute("errorMessage", "Nieprawidłowy kod BLIK! Wpisz dokładnie 6 cyfr.");
            return "payment";
        }

        return "redirect:/my-reservations?paid=true";
    }

    // 4. downloading tickets in PDF format
    @GetMapping("/reservations/{id}/pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable Long id, Principal principal) {
        //only the owner of the reservation can download its ticket
        Reservation reservation = reservationService.getReservationForUser(id, principal.getName());

        //additional security for downloading unpaid reservation
        if (reservation.getStatus() != ReservationStatus.PAID) {
            throw new IllegalStateException("Nie można pobrać biletu dla nieopłaconej rezerwacji");
        }

        //generating bytes of PDF file
        byte[] pdfBytes = pdfService.generateTicketPdf(reservation);

        //preparing headers of HTTP response
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Bilet_Kino_Rezerwacja_" + id + ".pdf");

        //returning answer with the bytes of the file and HTTP 200 OK code
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // 5. reservation view for logged user
    @GetMapping("/my-reservations")
    public String showMyReservations(Model model, Principal principal) {
        model.addAttribute("reservations", reservationService.getUserReservations(principal.getName()));
        return "my-reservations";
    }
}

package com.cinema.booking.controller;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.ReservationStatus;
import com.cinema.booking.repository.ReservationRepository;
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

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final PdfService pdfService;

    // 1. obsluga formularza z wybranmi miejscami pod /reserve
    @PostMapping("/reserve")
    public String processReservation(@RequestParam Long screeningId,
                                     @RequestParam(name = "seatIds", required = false) List<Long> seatIds,
                                     Principal principal){
        if (seatIds == null || seatIds.isEmpty()){
            return "redirect:/screenings/" + screeningId + "?error=no_seats";
        }

        String userEmail = principal != null ? principal.getName() : "guest@kino.pl";
        Reservation reservation = reservationService.createReservation(screeningId, seatIds, userEmail);

        //przekierwanie na strone platnosci BLIK
        return "redirect:/payment/" + reservation.getId();
    }

    // 2. widok formularza BLIK
    @GetMapping("/payment/{id}")
    public String showPaymentPage(@PathVariable Long id, Model model){
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Niepoprawny identyfikator rezerwacji"));

        if (reservation.getStatus() == ReservationStatus.PAID){
            return "redirect:/my-reservations";
        }

        double totalAmount = reservation.getTotalPrice() != null ? reservation.getTotalPrice().doubleValue() : 0.0;

        model.addAttribute("reservation", reservation);
        model.addAttribute("totalAmount", totalAmount);
        return "payment";
    }

    // 3. przetwarzanie platnosci BLIK
    @PostMapping("/payment/{id}/pay")
    public String processPayment(@PathVariable Long id,
                                 @RequestParam String blikCode,
                                 Model model) {
        boolean success = reservationService.processBlikPayment(id, blikCode);

        if (!success) {
            Reservation reservation = reservationRepository.findById(id).orElseThrow();
            double totalAmount = reservation.getTotalPrice() != null ? reservation.getTotalPrice().doubleValue() : 0.0;

            model.addAttribute("reservation", reservation);
            model.addAttribute("totalAmount", totalAmount);
            model.addAttribute("errorMessage", "Nieprawidłowy kod BLIK! Wpisz dokładnie 6 cyfr.");
            return "payment";
        }

        return "redirect:/my-reservations?paid=true";
    }

    // 4. pobieranie biletow w formacie PDF
    @GetMapping("/reservations/{id}/pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable Long id){
        //1. pobieranie rezerwacji z bazy
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono rezerwacji o id: " + id));

        // dodatkowe zabezpieczenie przed pobraniem nieoplaconej rezerwacji
        if (reservation.getStatus() != ReservationStatus.PAID) {
            throw new IllegalStateException("Nie można pobrać biletu dla nieopłaconej rezerwacji.");
        }

        //2. generowanie bajtow pliku PDF
        byte[] pdfBytes = pdfService.generateTicketPdf(reservation);

        //3. przygotowanie naglowkow odpowiedzi HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Bilet_Kino_Rezerwacja_" + id + ".pdf");

        //4. zwrocenie gotowej odpowiedzi z bajtami pliku i kodem HTTP 200 OK
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    // 5. widok rezerwacji
    @GetMapping("/my-reservations")
    public String showMyReservations(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        List<Reservation> userReservations = reservationRepository.findByCustomerEmailOrderByIdDesc(principal.getName());
        model.addAttribute("reservations", userReservations);
        return "my-reservations";
    }
}

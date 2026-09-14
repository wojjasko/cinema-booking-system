package com.cinema.booking.controller;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.repository.ReservationRepository;
import com.cinema.booking.service.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationRepository reservationRepository;
    private final PdfService pdfService;

    @GetMapping("/reservations/{id}/pdf")
    public ResponseEntity<byte[]> downloadTicketPdf(@PathVariable Long id){
        //1. pobieranie rezerwacji z bazy
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono rezerwacji o id: " + id));

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
}

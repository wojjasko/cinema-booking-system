package com.cinema.booking.service;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.Ticket;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generateTicketPdf(Reservation reservation){
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){

            // 1. inicjalizacja dokumentu PDF w pamięci RAM
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // dla polskich znakow trzeba zmienic czcionke
            PdfFont font;
            try {
                font = PdfFontFactory.createFont("c:/windows/fonts/arial.ttf", PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            }

            document.setFont(font);

            // 2. naglowek biletu
            Paragraph header = new Paragraph("BILET KINOWY")
                    .setFontSize(22)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(header);

            // 3. szczegoly rezerwacji i seansu
            document.add(new Paragraph("Numer rezerwacji: #" + reservation.getId()).setBold());
            document.add(new Paragraph("Film: " + reservation.getScreening().getMovie().getTitle()));
            document.add(new Paragraph("Sala: " + reservation.getScreening().getCinemaHall().getName()));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedDate = reservation.getScreening().getStartTime().format(formatter);
            document.add(new Paragraph("Data i godzina: " + formattedDate));
            document.add(new Paragraph("E-mail klienta: " + reservation.getCustomerEmail()));
            document.add(new Paragraph("Cena całkowita: " + reservation.getTotalPrice() + " PLN").setBold());

            document.add(new Paragraph("\nZAREZERWOWANE MIEJSCA: ").setBold());

            // 4. petla po biletach
            for (Ticket ticket : reservation.getTickets()) {
                document.add(new Paragraph("- Rząd: " + ticket.getSeat().getRowNumber() + ", Miejsce: " + ticket.getSeat().getSeatNumber()));
            }

            document.add(new Paragraph("\n"));

            // 5. generowanie kodu QR z ID rezerwacji
            byte[] qrCodeImage = generateQrCodeImage("RESERVATION-" + reservation.getId(), 150, 150);
            Image qrImage = new Image(ImageDataFactory.create(qrCodeImage));
            qrImage.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
            document.add(qrImage);

            document.add(new Paragraph("Prosimy o okazanie kodu QR przy wejściu na salę.")
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER));

            document.close();
            return baos.toByteArray();
        } catch (Exception e){
            throw new RuntimeException("Wystąpił błąd poczas generowania pliku PDF", e);
        }
    }

    // metoda generujaca kod QR jako tablice bajtow (obrazek PNG)
    private byte[] generateQrCodeImage(String text, int width, int height) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        return pngOutputStream.toByteArray();
    }
}

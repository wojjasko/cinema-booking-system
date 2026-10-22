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
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int QR_SIZE_PX = 150;

    public byte[] generateTicketPdf(Reservation reservation){
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream()){

            // pdf document build in RAM memory
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document document = new Document(pdfDoc);
            document.setFont(createPolishFont());

            addHeader(document);
            addReservationDetails(document, reservation);
            addSeatList(document, reservation);
            addQrCode(document, reservation);

            document.close();
            return baos.toByteArray();
        } catch (Exception e){
            throw new RuntimeException("Wystąpił błąd poczas generowania pliku PDF", e);
        }
    }

    private PdfFont createPolishFont() throws IOException{
        return PdfFontFactory.createFont(StandardFonts.HELVETICA, PdfEncodings.CP1250);
    }

    private void addHeader(Document document){
        document.add(new Paragraph("BILET KINOWY")
                .setFontSize(22)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addReservationDetails(Document document, Reservation reservation){
        document.add(new Paragraph("Numer rezerwacji: #" + reservation.getId()).setBold());
        document.add(new Paragraph("Film: " + reservation.getScreening().getMovie().getTitle()));
        document.add(new Paragraph("Sala: " + reservation.getScreening().getCinemaHall().getName()));
        document.add(new Paragraph("Data i godzina: " + reservation.getScreening().getStartTime().format(DATE_TIME_FORMAT)));
        document.add(new Paragraph("E-mail klienta: " + reservation.getCustomerEmail()));
        document.add(new Paragraph("Cena całkowita: " + reservation.getTotalPrice() + " PLN").setBold());
    }

    private void addSeatList(Document document, Reservation reservation){
        document.add(new Paragraph("ZAREZERWOWANE MIEJSCA: ").setBold().setMarginTop(15));

        for(Ticket ticket : reservation.getTickets()){
            document.add(new Paragraph("- Rząd: " + ticket.getSeat().getRowNumber()
            + ", Miejsce: " + ticket.getSeat().getSeatNumber()));
        }
    }

    private void addQrCode(Document document, Reservation reservation) throws Exception{
        byte[] qrCodeImage = generateQrCodeImage("RESERVATION-" + reservation.getId(), QR_SIZE_PX, QR_SIZE_PX);

        Image qrImage = new Image(ImageDataFactory.create(qrCodeImage));
        qrImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
        qrImage.setMarginTop(15);
        document.add(qrImage);

        document.add(new Paragraph("Prosimy o okazanie kodu QR przy wejściu na salę.")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER));
    }

    // generate QR code as byte array (PNG image)
    private byte[] generateQrCodeImage(String text, int width, int height) throws Exception {
        BitMatrix bitMatrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, width, height);

        try (ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        }
    }
}

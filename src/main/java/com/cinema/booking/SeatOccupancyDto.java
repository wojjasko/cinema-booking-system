package com.cinema.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatOccupancyDto {
    private Long ticketId;
    private String customerEmail;
    private LocalDateTime reservationTime;
}
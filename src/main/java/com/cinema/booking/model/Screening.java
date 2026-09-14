package com.cinema.booking.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "screenings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Screening {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // jeden film moze miec wiele seansow
    // lazy - chroni przed niepotrzebnym dociazeniam bazy danych, pobierze tylko to o co prosimy np jak prosimy o get to tylko get wezmie
    @JoinColumn(name = "movie_id", nullable = false) // tworzy w tabeli screenings kolumne z kluczem obcym o nazwie movie_id, ktora laczy sie z ID z tabeli movies
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY) // jedna sala moze miec wiele seansow
    @JoinColumn(name = "cinema_hall_id", nullable = false)
    private CinemaHall cinemaHall;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private BigDecimal price;
}

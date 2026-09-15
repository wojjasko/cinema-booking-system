package com.cinema.booking.config;

import com.cinema.booking.model.*;
import com.cinema.booking.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    //specjalny interfejs w Spring Boot - kod zawarty w jego metodzie wykonuje sie automatycznie zaraz po uruchomieniu aplikacji.
    CommandLineRunner initDatabase(MovieRepository movieRepository,
                                   CinemaHallRepository cinemaHallRepository,
                                   ScreeningRepository screeningRepository,
                                   SeatRepository seatRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder){
        return args -> {

            // UZYTKOWNICY
            if(!userRepository.existsByEmail("admin@kino.pl")){
                User admin = User.builder()
                        .email("admin@kino.pl")
                        .password(passwordEncoder.encode("admin123"))
                        .firstName("Main")
                        .lastName("Admin")
                        .phoneNumber("123456789")
                        .role(Role.ROLE_ADMIN)
                        .build();

                userRepository.save(admin);
            }

            if(!userRepository.existsByEmail("user@kino.pl")){
                User user = User.builder()
                        .email("user@kino.pl")
                        .password(passwordEncoder.encode("user123"))
                        .firstName("Jan")
                        .lastName("Kowalski")
                        .phoneNumber("987654321")
                        .role(Role.ROLE_USER)
                        .build();

                userRepository.save(user);
            }

            if(!userRepository.existsByEmail("sigma@kino.pl")){
                User user = User.builder()
                        .email("sigma@kino.pl")
                        .password(passwordEncoder.encode("sigma123"))
                        .firstName("Łukasz")
                        .lastName("Sigmowski")
                        .phoneNumber("333666111")
                        .role(Role.ROLE_USER)
                        .build();

                userRepository.save(user);
            }

            if(movieRepository.count() == 0){

                // FILMY
                Movie movie1 = Movie.builder()
                        .title("Incepcja")
                        .description("Złodziej, który kradnie tajemnice poprzez wykorzystanie technologii dzielenia snów.")
                        .durationMinutes(148)
                        .genre("Sci-Fi")
                        .posterUrl("https://fwcdn.pl/fpo/08/91/500891/7354571_1.10.webp")
                        .build();

                Movie movie2 = Movie.builder()
                        .title("Diuna")
                        .description("Książę Paul Atryda prowadzi koczownicze plemiona do walki o pustynną planetę.")
                        .durationMinutes(155)
                        .genre("Sci-Fi")
                        .posterUrl("https://fwcdn.pl/fpo/94/76/469476/8030930_1.3.jpg")
                        .build();

                Movie movie3 = Movie.builder()
                        .title("Interstellar")
                        .description("Byt ludzkości na Ziemi dobiega końca wskutek zmian klimatycznych. Grupa naukowców odkrywa tunel czasoprzestrzenny, który umożliwia poszukiwanie nowego domu.")
                        .durationMinutes(169)
                        .genre("Sci-Fi")
                        .posterUrl("https://fwcdn.pl/fpo/56/29/375629/7670122_2.10.webp")
                        .build();

                Movie movie4 = Movie.builder()
                        .title("Mroczny Rycerz")
                        .description("Batman, z pomocą porucznika Gordona oraz prokuratora Harveya Denta, występuje przeciwko przerażającemu i nieobliczalnemu Jokerowi, który chce pogrążyć Gotham City w chaosie.")
                        .durationMinutes(152)
                        .genre("Akcja")
                        .posterUrl("https://fwcdn.pl/fpo/63/51/236351/7198307_2.10.webp")
                        .build();

                Movie movie5 = Movie.builder()
                        .title("Oppenheimer")
                        .description("Historia amerykańskiego naukowca J. Roberta Oppenheimera i jego roli w stworzeniu bomby atomowej.")
                        .durationMinutes(180)
                        .genre("Dramat / Biograficzny")
                        .posterUrl("https://fwcdn.pl/fpo/28/17/10002817/8072064_2.10.webp")
                        .build();

                movie1 = movieRepository.save(movie1);
                movie2 = movieRepository.save(movie2);
                movie3 = movieRepository.save(movie3);
                movie4 = movieRepository.save(movie4);
                movie5 = movieRepository.save(movie5);

                // SALE KINOWE
                CinemaHall hall1 = CinemaHall.builder()
                        .name("Sala 1 - IMAX")
                        .totalRows(10)
                        .seatsPerRow(15)
                        .build();

                CinemaHall hall2 = CinemaHall.builder()
                        .name("Sala 2 - 3D Dolby")
                        .totalRows(8)
                        .seatsPerRow(12)
                        .build();

                CinemaHall hall3 = CinemaHall.builder()
                        .name("Sala 3 - VIP Studio")
                        .totalRows(6)
                        .seatsPerRow(4)
                        .build();

                hall1 = cinemaHallRepository.save(hall1);
                hall2 = cinemaHallRepository.save(hall2);
                hall3 = cinemaHallRepository.save(hall3);

                // GENEROWANIE MIEJSC DLA SAL

                List<Seat> seats1 = new ArrayList<>();
                for (int row = 1; row <= hall1.getTotalRows(); row++) {
                    for (int seatNum = 1; seatNum <= hall1.getSeatsPerRow(); seatNum++) {
                        seats1.add(Seat.builder()
                                .rowNumber(row)
                                .seatNumber(seatNum)
                                .cinemaHall(hall1)
                                .build());
                    }
                }

                List<Seat> seats2 = new ArrayList<>();
                for (int row = 1; row <= hall2.getTotalRows(); row++) {
                    for (int seatNum = 1; seatNum <= hall2.getSeatsPerRow(); seatNum++) {
                        seats2.add(Seat.builder()
                                .rowNumber(row)
                                .seatNumber(seatNum)
                                .cinemaHall(hall2)
                                .build());
                    }
                }

                List<Seat> seats3 = new ArrayList<>();
                for (int row = 1; row <= hall3.getTotalRows(); row++) {
                    for (int seatNum = 1; seatNum <= hall3.getSeatsPerRow(); seatNum++) {
                        seats3.add(Seat.builder()
                                .rowNumber(row)
                                .seatNumber(seatNum)
                                .cinemaHall(hall3)
                                .build());
                    }
                }

                seatRepository.saveAll(seats1);
                seatRepository.saveAll(seats2);
                seatRepository.saveAll(seats3);

                // TWORZENIE SEANSOW
                LocalDateTime now = LocalDateTime.now();

                List<Screening> screenings = List.of(
                        // Incepcja
                        Screening.builder()
                                .movie(movie1)
                                .cinemaHall(hall1)
                                .startTime(now.plusDays(1).withHour(17).withMinute(30))
                                .price(new BigDecimal("28.00"))
                                .build(),
                        Screening.builder()
                                .movie(movie1)
                                .cinemaHall(hall2)
                                .startTime(now.plusDays(2).withHour(20).withMinute(45))
                                .price(new BigDecimal("25.00"))
                                .build(),

                        // Diuna
                        Screening.builder()
                                .movie(movie2)
                                .cinemaHall(hall1)
                                .startTime(now.plusDays(1).withHour(20).withMinute(30))
                                .price(new BigDecimal("32.00"))
                                .build(),
                        Screening.builder()
                                .movie(movie2)
                                .cinemaHall(hall3)
                                .startTime(now.plusDays(3).withHour(18).withMinute(00))
                                .price(new BigDecimal("35.00"))
                                .build(),

                        // Interstellar
                        Screening.builder()
                                .movie(movie3)
                                .cinemaHall(hall1)
                                .startTime(now.plusDays(2).withHour(16).withMinute(00))
                                .price(new BigDecimal("30.00"))
                                .build(),
                        Screening.builder()
                                .movie(movie3)
                                .cinemaHall(hall2)
                                .startTime(now.plusDays(4).withHour(19).withMinute(15))
                                .price(new BigDecimal("26.50"))
                                .build(),

                        // Mroczny Rycerz
                        Screening.builder()
                                .movie(movie4)
                                .cinemaHall(hall2)
                                .startTime(now.plusDays(1).withHour(19).withMinute(00))
                                .price(new BigDecimal("24.00"))
                                .build(),
                        Screening.builder()
                                .movie(movie4)
                                .cinemaHall(hall3)
                                .startTime(now.plusDays(3).withHour(21).withMinute(15))
                                .price(new BigDecimal("34.00"))
                                .build(),

                        // Oppenheimer
                        Screening.builder()
                                .movie(movie5)
                                .cinemaHall(hall1)
                                .startTime(now.plusDays(3).withHour(16).withMinute(30))
                                .price(new BigDecimal("31.00"))
                                .build(),
                        Screening.builder()
                                .movie(movie5)
                                .cinemaHall(hall2)
                                .startTime(now.plusDays(4).withHour(20).withMinute(00))
                                .price(new BigDecimal("27.00"))
                                .build()
                );

                screeningRepository.saveAll(screenings);
            }
        };
    }
}
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

            if(movieRepository.count() == 0){

                // -- TWORZENIE FILMOW --
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

                movie1 = movieRepository.save(movie1);
                movie2 = movieRepository.save(movie2);

                // -- TWORZENIE SAL KINOWYCH --
                CinemaHall hall1 = CinemaHall.builder()
                        .name("Sala 1 - IMAX")
                        .totalRows(10)
                        .seatsPerRow(15)
                        .build();
                hall1 = cinemaHallRepository.save(hall1);

                CinemaHall hall2 = CinemaHall.builder()
                        .name("Sala 2 - 3D")
                        .totalRows(8)
                        .seatsPerRow(10)
                        .build();
                hall2 = cinemaHallRepository.save(hall2);

                List<Seat> seats1 = new ArrayList<>();
                for(int row = 1; row <= hall1.getTotalRows(); row++){
                    for(int seatNum = 1; seatNum <= hall1.getSeatsPerRow(); seatNum++){
                        Seat seat = Seat.builder()
                                .rowNumber(row)
                                .seatNumber(seatNum)
                                .cinemaHall(hall1)
                                .build();
                        seats1.add(seat);
                    }
                }

                List<Seat> seats2 = new ArrayList<>();
                for(int row = 1; row <= hall2.getTotalRows(); row++){
                    for(int seatNum = 1; seatNum <= hall2.getSeatsPerRow(); seatNum++){
                        Seat seat = Seat.builder()
                                .rowNumber(row)
                                .seatNumber(seatNum)
                                .cinemaHall(hall2)
                                .build();
                        seats2.add(seat);
                    }
                }

                seatRepository.saveAll(seats1); // zapisujemy cala liste foteli naraz, zamiast w petli wysylac 40 zapytan, wysylamy tylko 1 na koncu
                seatRepository.saveAll(seats2);

                // -- TWORZENIE SEANSOW --
                Screening screening1 = Screening.builder()
                        .movie(movie1)
                        .cinemaHall(hall1)
                        .startTime(LocalDateTime.now().plusDays(1).withHour(18).withMinute(0))
                        .price(new BigDecimal("25.50"))
                        .build();

                Screening screening2 = Screening.builder()
                        .movie(movie2)
                        .cinemaHall(hall2)
                        .startTime(LocalDateTime.now().plusDays(1).withHour(21).withMinute(0))
                        .build();

                screeningRepository.save(screening1);
                screeningRepository.save(screening2);
            }
        };
    }
}
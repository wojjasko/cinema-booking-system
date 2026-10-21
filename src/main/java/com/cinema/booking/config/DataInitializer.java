package com.cinema.booking.config;

import com.cinema.booking.model.*;
import com.cinema.booking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
// CommandLineRunner - code from method run() starts automatically just after launching the application
public class DataInitializer implements CommandLineRunner{

    private final MovieRepository movieRepository;
    private final CinemaHallRepository cinemaHallRepository;
    private final ScreeningRepository screeningRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createUserIfMissing("admin@kino.pl", "admin123", "Main", "Admin", "123456789", Role.ROLE_ADMIN);
        createUserIfMissing("user@kino.pl", "user123", "Jan", "Kowalski", "987654321", Role.ROLE_USER);
        createUserIfMissing("otheruser@kino.pl", "otheruser123", "Łukasz", "Nowakowski", "333666111", Role.ROLE_USER);

        if (movieRepository.count() == 0) {
            createMoviesHallsAndScreenings();
        }

        rescheduleScreeningsIfAllExpired();
    }

    // USERS

    private void createUserIfMissing(String email, String rawPassword, String firstName,
                                     String lastName, String phoneNumber, Role role) {
        if (userRepository.existsByEmail(email)) {
            return;
        }

        userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .role(role)
                .build());
    }

    // MOVIES, HALLS, SCREENINGS

    private void createMoviesHallsAndScreenings() {
        Movie inception = saveMovie("Incepcja",
                "Złodziej, który kradnie tajemnice poprzez wykorzystanie technologii dzielenia snów.",
                148, "Sci-Fi", "https://fwcdn.pl/fpo/08/91/500891/7354571_1.10.webp");

        Movie dune = saveMovie("Diuna",
                "Książę Paul Atryda prowadzi koczownicze plemiona do walki o pustynną planetę.",
                155, "Sci-Fi", "https://fwcdn.pl/fpo/94/76/469476/8030930_1.3.jpg");

        Movie interstellar = saveMovie("Interstellar",
                "Byt ludzkości na Ziemi dobiega końca wskutek zmian klimatycznych. Grupa naukowców odkrywa tunel czasoprzestrzenny, który umożliwia poszukiwanie nowego domu.",
                169, "Sci-Fi", "https://fwcdn.pl/fpo/56/29/375629/7670122_2.10.webp");

        Movie darkKnight = saveMovie("Mroczny Rycerz",
                "Batman, z pomocą porucznika Gordona oraz prokuratora Harveya Denta, występuje przeciwko przerażającemu i nieobliczalnemu Jokerowi, który chce pogrążyć Gotham City w chaosie.",
                152, "Akcja", "https://fwcdn.pl/fpo/63/51/236351/7198307_2.10.webp");

        Movie oppenheimer = saveMovie("Oppenheimer",
                "Historia amerykańskiego naukowca J. Roberta Oppenheimera i jego roli w stworzeniu bomby atomowej.",
                180, "Dramat / Biograficzny", "https://fwcdn.pl/fpo/28/17/10002817/8072064_2.10.webp");

        CinemaHall imax = saveHallWithSeats("Sala 1 - IMAX", 10, 15);
        CinemaHall dolby = saveHallWithSeats("Sala 2 - 3D Dolby", 8, 12);
        CinemaHall vip = saveHallWithSeats("Sala 3 - VIP Studio", 6, 4);

        screeningRepository.saveAll(List.of(
                //Inception
                screening(inception, imax, 1, 17, 30, "28.00"),
                screening(inception, dolby, 2, 20, 45, "25.00"),

                //Dune
                screening(dune, imax, 1, 20, 30, "32.00"),
                screening(dune, vip, 3, 18, 0, "35.00"),

                //Interstellar
                screening(interstellar, imax, 2, 16, 0, "30.00"),
                screening(interstellar, dolby, 4, 19, 15, "26.50"),

                // Dark Knight
                screening(darkKnight, dolby, 1, 19, 0, "24.00"),
                screening(darkKnight, vip, 3, 21, 15, "34.00"),

                // Oppenheimer
                screening(oppenheimer, imax, 3, 16, 30, "31.00"),
                screening(oppenheimer, dolby, 4, 20, 0, "27.00")
        ));
    }

    private Movie saveMovie(String title, String description, int durationMinutes, String genre, String posterUrl) {
        return movieRepository.save(Movie.builder()
                .title(title)
                .description(description)
                .durationMinutes(durationMinutes)
                .genre(genre)
                .posterUrl(posterUrl)
                .build());
    }

    private CinemaHall saveHallWithSeats(String name, int totalRows, int seatsPerRow) {
        CinemaHall hall = cinemaHallRepository.save(CinemaHall.builder()
                .name(name)
                .totalRows(totalRows)
                .seatsPerRow(seatsPerRow)
                .build());

        List<Seat> seats = new ArrayList<>();
        for (int row = 1; row <= totalRows; row++) {
            for (int seatNumber = 1; seatNumber <= seatsPerRow; seatNumber++) {
                seats.add(Seat.builder()
                        .rowNumber(row)
                        .seatNumber(seatNumber)
                        .cinemaHall(hall)
                        .build());
            }
        }
        seatRepository.saveAll(seats);

        return hall;
    }

    // screening starting in a "daysFromToday" days at specific hour
    private Screening screening(Movie movie, CinemaHall hall, int daysFromToday, int hour, int minute, String price) {
        return Screening.builder()
                .movie(movie)
                .cinemaHall(hall)
                .startTime(LocalDate.now().plusDays(daysFromToday).atTime(hour, minute))
                .price(new BigDecimal(price))
                .build();
    }

    // REFRESHING THE DATES OF SCREENINGS

    // if there are only outdated screenings in the database - we move them all to the future
    // we keep hours and seats only, the earliest screening will be tomorrow
    private void rescheduleScreeningsIfAllExpired() {
        List<Screening> allScreenings = screeningRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        if (allScreenings.isEmpty()
                || !screeningRepository.findByStartTimeAfterOrderByStartTimeAsc(now).isEmpty()) {
            return;
        }

        LocalDateTime earliest = allScreenings.stream()
                .map(Screening::getStartTime)
                .min(LocalDateTime::compareTo)
                .orElse(now);

        long shiftDays = ChronoUnit.DAYS.between(earliest.toLocalDate(), now.toLocalDate()) + 1;

        allScreenings.forEach(s -> s.setStartTime(s.getStartTime().plusDays(shiftDays)));
        screeningRepository.saveAll(allScreenings);
    }
}

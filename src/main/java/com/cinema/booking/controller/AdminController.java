package com.cinema.booking.controller;

import com.cinema.booking.model.Movie;
import com.cinema.booking.model.Screening;
import com.cinema.booking.model.Seat;
import com.cinema.booking.model.Ticket;
import com.cinema.booking.SeatOccupancyDto;
import com.cinema.booking.repository.CinemaHallRepository;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.ReservationRepository;
import com.cinema.booking.repository.ScreeningRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.repository.TicketRepository;
import com.cinema.booking.service.ScreeningService;
import com.cinema.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin") //prefiks dla wszystkich linkow np samo GetMapping daje z automatu /admin
@RequiredArgsConstructor
public class AdminController {

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;
    private final CinemaHallRepository cinemaHallRepository;
    private final ScreeningService screeningService;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ReservationService reservationService;
    private final TicketRepository ticketRepository;


    //1. glowna strona panelu admina (lista filmow i seansow)
    @GetMapping
    public String showAdminDashboard(Model model){
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("screenings", screeningService.getAllScreenings());
        model.addAttribute("reservations", reservationRepository.findAll());
        return "admin/dashboard";
    }

    //2a. formularz dodawania nowego filmu
    @GetMapping("/movies/add")
    public String showAddMovieForm(Model model){
        model.addAttribute("movie", new Movie());
        return "admin/add-movie";
    }

    //2b. zapis nowego filmu
    @PostMapping("/movies/add")
    public String processAddMovie(@ModelAttribute("movie") Movie movie){
        movieRepository.save(movie);
        return "redirect:/admin";
    }

    //2c. usuwanie filmu po id
    @PostMapping("/movies/delete/{id}")
    public String deleteMovie(@PathVariable Long id){
        movieRepository.deleteById(id);
        return "redirect:/admin";
    }

    //3a. formularz planowania nowego seansu
    @GetMapping("/screenings/add")
    public String showAddScreeningForm(Model model){
        model.addAttribute("screening", new Screening());
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("halls", cinemaHallRepository.findAll());
        return "admin/add-screening";
    }

    //3b. zapis nowego seansu do bazy
    @PostMapping("/screenings/add")
    public String processAddScreening(@ModelAttribute("screening") Screening screening, Model model){

        // 1. pobieranie pelnych danych wybranego filmu z bazy aby poznac jego czas trwania
        Movie movie = movieRepository.findById(screening.getMovie().getId())
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy film"));

        LocalDateTime newStart = screening.getStartTime();
        LocalDateTime newEnd = newStart.plusMinutes(movie.getDurationMinutes());

        // 2. weryfikacja w bazie czy w tej samej sali nie ma juz innego nakladajacego sie seansu
        boolean isOverlapping = screeningRepository.existsOverlappingScreening(
                screening.getCinemaHall().getId(),
                newStart,
                newEnd
        );

        // 3. jesli sie nakladaja - przerywamy zapis i wracamy do formularza z bledem
        if(isOverlapping){
            model.addAttribute("errorMessage", "Wybrana sala jest zajęta o danej porze. Wybierz inną godzinę lub salę.");
            model.addAttribute("movies", movieRepository.findAll());
            model.addAttribute("halls", cinemaHallRepository.findAll());
            return "admin/add-screening";
        }

        // 4. jesli sie nie nakladaja - zapisujemy nowy seans do bazy
        screening.setMovie(movie);
        screeningRepository.save(screening);
        return "redirect:/admin";


    }

    //3c. usuwanie seansu po ID
    @PostMapping("/screenings/delete/{id}")
    public String deleteScreening(@PathVariable Long id){
        screeningRepository.deleteById(id);
        return "redirect:/admin";
    }

    // podglad miejsc na sali dla konkretnego seansu
    @GetMapping("/screenings/{id}/seats")
    public String showScreeningSeatsAdmin(@PathVariable Long id, Model model){
        Screening screening = screeningService.getScreeningById(id);

        List<Seat> seats = seatRepository.findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(
                screening.getCinemaHall().getId()
        );

        List<Ticket> tickets = ticketRepository.findByReservationScreeningId(id);

        // Bezpieczne mapowanie DTO z zabezpieczeniem przed NullPointerException i LazyLoading
        Map<Long, SeatOccupancyDto> seatTicketMap = tickets.stream()
                .filter(ticket -> ticket.getSeat() != null)
                .collect(Collectors.toMap(
                        ticket -> ticket.getSeat().getId(),
                        ticket -> {
                            String email = (ticket.getReservation() != null && ticket.getReservation().getCustomerEmail() != null)
                                    ? ticket.getReservation().getCustomerEmail()
                                    : "Brak danych";

                            LocalDateTime resTime = (ticket.getReservation() != null)
                                    ? ticket.getReservation().getReservationTime()
                                    : LocalDateTime.now();

                            return new SeatOccupancyDto(ticket.getId(), email, resTime);
                        },
                        (t1, t2) -> t1
                ));

        model.addAttribute("screening", screening);
        model.addAttribute("seats", seats);
        model.addAttribute("seatTicketMap", seatTicketMap);

        return "admin/screening-seats";
    }

    //anulowanie pojedynczego miejsca rezerwacji uzytkownika
    @PostMapping("/tickets/cancel-single/{ticketId}")
    public String cancelSingleTicket(@PathVariable Long ticketId, @RequestParam Long screeningId) {
        reservationService.cancelSingleTicket(ticketId);
        return "redirect:/admin/screenings/" + screeningId + "/seats";
    }

    //anulowanie wszystkich miejsc rezerwacji uzytkownika
    @PostMapping("/tickets/cancel-all")
    public String cancelAllUserTickets(@RequestParam Long screeningId, @RequestParam String customerEmail) {
        reservationService.cancelAllUserTicketsForScreening(screeningId, customerEmail);
        return "redirect:/admin/screenings/" + screeningId + "/seats";
    }
}
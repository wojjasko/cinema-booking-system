package com.cinema.booking.controller;

import com.cinema.booking.model.Movie;
import com.cinema.booking.model.Screening;
import com.cinema.booking.model.Seat;
import com.cinema.booking.repository.CinemaHallRepository;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.ScreeningRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.service.ScreeningService;
import com.cinema.booking.service.ReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin") //prefix for all links, for example: GetMapping alone gives /admin automatically
@RequiredArgsConstructor
public class AdminController {

    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;
    private final CinemaHallRepository cinemaHallRepository;
    private final SeatRepository seatRepository;
    private final ScreeningService screeningService;
    private final ReservationService reservationService;


    //1. admin panel - main page (schedule of screenings)
    @GetMapping
    public String showAdminDashboard(Model model){
        model.addAttribute("screenings", screeningService.getAllScreenings());
        return "admin/dashboard";
    }

    //2a. form - adding new film
    @GetMapping("/movies/add")
    public String showAddMovieForm(Model model){
        model.addAttribute("movie", new Movie());
        return "admin/add-movie";
    }

    //2b. save - new movie
    @PostMapping("/movies/add")
    public String processAddMovie(@ModelAttribute("movie") Movie movie){
        movieRepository.save(movie);
        return "redirect:/admin";
    }

    //2c. removing movie with specific id
    @PostMapping("/movies/delete/{id}")
    public String deleteMovie(@PathVariable Long id){
        movieRepository.deleteById(id);
        return "redirect:/admin";
    }

    //3a. form - planning new screening
    @GetMapping("/screenings/add")
    public String showAddScreeningForm(Model model){
        model.addAttribute("screening", new Screening());
        populateScreeningFormOptions(model);
        return "admin/add-screening";
    }

    //3b. save - new screening to the database
    @PostMapping("/screenings/add")
    public String processAddScreening(@ModelAttribute("screening") Screening screening, Model model){

        // 1. download the full data of chosen movie from the database, just to get its length
        Movie movie = movieRepository.findById(screening.getMovie().getId())
                .orElseThrow(() -> new IllegalArgumentException("Nieprawidłowy film"));

        LocalDateTime newStart = screening.getStartTime();
        LocalDateTime newEnd = newStart.plusMinutes(movie.getDurationMinutes());

        // 2. veryfying in the database if there is overlapping in scheduled screenings
        boolean isOverlapping = screeningRepository.existsOverlappingScreening(
                screening.getCinemaHall().getId(),
                newStart,
                newEnd
        );

        // 3. if the overlapping occurs - breaking save method and get back to the form with returing an error
        if(isOverlapping){
            model.addAttribute("errorMessage", "Wybrana sala jest zajęta o danej porze. Wybierz inną godzinę lub salę.");
            populateScreeningFormOptions(model);
            return "admin/add-screening";
        }

        // 4. no overlapping - we asve the screening to the database
        screening.setMovie(movie);
        screeningRepository.save(screening);
        return "redirect:/admin";
    }

    //3c. removing the screening after ID
    @PostMapping("/screenings/delete/{id}")
    public String deleteScreening(@PathVariable Long id){
        screeningRepository.deleteById(id);
        return "redirect:/admin";
    }

    //4. preview of the seats for chosen movie
    @GetMapping("/screenings/{id}/seats")
    public String showScreeningSeatsAdmin(@PathVariable Long id, Model model) {
        Screening screening = screeningService.getScreeningById(id);

        List<Seat> seats = seatRepository.findByCinemaHallIdOrderByRowNumberAscSeatNumberAsc(
                screening.getCinemaHall().getId()
        );

        model.addAttribute("screening", screening);
        model.addAttribute("seats", seats);
        model.addAttribute("seatTicketMap", reservationService.getSeatOccupancyBySeatId(id));

        return "admin/screening-seats";
    }

    //5a. cancellling one-seat user's reservation
    @PostMapping("/tickets/cancel-single/{ticketId}")
    public String cancelSingleTicket(@PathVariable Long ticketId, @RequestParam Long screeningId) {
        reservationService.cancelSingleTicket(ticketId);
        return "redirect:/admin/screenings/" + screeningId + "/seats";
    }

    //5b. cancelling all-seats user's reservation
    @PostMapping("/tickets/cancel-all")
    public String cancelAllUserTickets(@RequestParam Long screeningId, @RequestParam String customerEmail) {
        reservationService.cancelAllUserTicketsForScreening(screeningId, customerEmail);
        return "redirect:/admin/screenings/" + screeningId + "/seats";
    }

    // the lists of movies and halls required in screening reservation form
    private void populateScreeningFormOptions(Model model) {
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("halls", cinemaHallRepository.findAll());
    }
}
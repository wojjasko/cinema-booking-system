package com.cinema.booking.controller;

import com.cinema.booking.model.Movie;
import com.cinema.booking.model.Screening;
import com.cinema.booking.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ScreeningService screeningService;

    @GetMapping("/")
    public String showHomePage(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {

        List<Screening> screenings = (date != null)
                ? screeningService.getScreeningsForDate(date)
                : screeningService.getUpcomingScreenings();

        // grouping screenings after movie: screenings are already sorted chronologically through repo,
        // LinedHashMap keep the order, so additional sorting is not needed
        Map<Movie, List<Screening>> screeningsByMovie = screenings.stream()
                .collect(Collectors.groupingBy(
                        Screening::getMovie,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        LocalDate today = LocalDate.now();

        model.addAttribute("screeningsByMovie", screeningsByMovie);
        model.addAttribute("selectedDate", date);
        model.addAttribute("today", today);
        model.addAttribute("tomorrow", today.plusDays(1));

        return "index";
    }
}

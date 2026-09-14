package com.cinema.booking.controller;

import com.cinema.booking.model.Movie;
import com.cinema.booking.model.Screening;
import com.cinema.booking.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final ScreeningService screeningService;

    @GetMapping("/")
    public String showHomePage(Model model) {
        List<Screening> screenings = screeningService.getAllScreenings();

        // grupowanie seansow po obiekcie Movie z zachowaniem kolejności (LinkedHashMap)
        Map<Movie, List<Screening>> screeningsByMovie = screenings.stream()
                .collect(Collectors.groupingBy(
                        Screening::getMovie,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // sortowanie seansow chronologicznie dla kazdego filmu
        screeningsByMovie.forEach((movie, screeningList) ->
                screeningList.sort(Comparator.comparing(Screening::getStartTime))
        );

        model.addAttribute("screeningsByMovie", screeningsByMovie);
        return "index";
    }
}
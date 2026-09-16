package com.cinema.booking.controller;

import com.cinema.booking.model.Movie;
import com.cinema.booking.model.Screening;
import com.cinema.booking.repository.MovieRepository;
import com.cinema.booking.repository.ScreeningRepository;
import com.cinema.booking.service.ScreeningService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class MovieController {

    private final ScreeningService screeningService;
    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;

    @GetMapping("/")
    public String showHomePage(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               Model model) {

        List<Screening> screenings;

        if (date != null){
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
            screenings = screeningRepository.findByStartTimeBetweenOrderByStartTimeAsc(startOfDay, endOfDay);
        } else {
            screenings = screeningRepository.findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now());
        }

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
        model.addAttribute("selectedDate", date);
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("tomorrow", LocalDate.now().plusDays(1));

        return "index";
    }
}
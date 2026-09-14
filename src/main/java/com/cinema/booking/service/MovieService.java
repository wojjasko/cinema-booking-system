package com.cinema.booking.service;


import com.cinema.booking.model.Movie;
import com.cinema.booking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service // rejestruje te klase jako komponent logiki biznesowej.
@RequiredArgsConstructor // automatycznie generuje konstruktor dla wszystkich pól oznaczonych jako final
public class MovieService {

    private final MovieRepository movieRepository;

    public List<Movie> getAllMovies(){
        return movieRepository.findAll(); // SELECT * FROM movies
    }

    public Movie getMovieById(Long id){
        return movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono filmu o podanym id."));
    }

}

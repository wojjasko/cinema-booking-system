package com.cinema.booking.service;

import com.cinema.booking.model.Screening;
import com.cinema.booking.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScreeningService {

    private final ScreeningRepository screeningRepository;

    public List<Screening> getAllScreenings(){
        return screeningRepository.findAll();
    }

    public Screening getScreeningById(Long id){
        return screeningRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono seansu o id: " + id));
    }
}

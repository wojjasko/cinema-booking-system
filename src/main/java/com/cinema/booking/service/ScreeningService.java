package com.cinema.booking.service;

import com.cinema.booking.model.Screening;
import com.cinema.booking.repository.ScreeningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public List<Screening> getUpcomingScreenings(){
        return screeningRepository.findByStartTimeAfterOrderByStartTimeAsc(LocalDateTime.now());
    }

    public List<Screening> getScreeningsForDate(LocalDate date){
        return screeningRepository.findByStartTimeBetweenOrderByStartTimeAsc(
                date.atStartOfDay(),
                date.atTime(LocalTime.MAX));
    }

}

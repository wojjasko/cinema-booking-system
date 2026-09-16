package com.cinema.booking.service;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.ReservationStatus;
import com.cinema.booking.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupScheduler {

    private final ReservationRepository reservationRepository;

    // minuta
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void cancelExpiredReservations(){
        //po 5 minutach
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(1);

        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndReservationTimeBefore(ReservationStatus.PENDING, cutoffTime);

        if (!expiredReservations.isEmpty()) {
            for (Reservation reservation : expiredReservations) {
                reservation.setStatus(ReservationStatus.CANCELLED);

                reservation.getTickets().clear();

                log.info("Anulowano przeterminowaną rezerwację ID: {}. Fotele zostały zwolnione.", reservation.getId());
            }

            reservationRepository.saveAllAndFlush(expiredReservations);
        }

    }

}

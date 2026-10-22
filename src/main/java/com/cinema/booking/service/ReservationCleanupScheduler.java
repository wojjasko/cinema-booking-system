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

    private static final long PAYMENT_TIMEOUT_MINUTES = 2;

    private static final long CHECK_INTERVAL_MS = 30_000;

    private final ReservationRepository reservationRepository;

    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    @Transactional
    public void cancelExpiredReservations(){
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndReservationTimeBefore(ReservationStatus.PENDING, cutoffTime);

        if (expiredReservations.isEmpty()) {
            return;
        }

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            // orphanRemoval removes tickets from database - the seats are free again thanks to that
            reservation.getTickets().clear();

            log.info("Anulowano przeterminowaną rezerwację ID: {}. Fotele zostały zwolnione.", reservation.getId());
        }

        reservationRepository.saveAllAndFlush(expiredReservations);

    }

}

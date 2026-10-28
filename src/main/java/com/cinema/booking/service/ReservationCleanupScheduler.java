package com.cinema.booking.service;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.ReservationStatus;
import com.cinema.booking.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationCleanupScheduler {

    private static final long PAYMENT_TIMEOUT_MINUTES = 2;

    private static final long CHECK_INTERVAL_MS = 30_000;

    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;

    //no @Transactional here on purpose, bcs every reservation is cancelled in its own short transaction
    //(inside ReservationService), so one conflict (for example the user has just paid) does not roll back
    //the cancellation of all the others
    @Scheduled(fixedRate = CHECK_INTERVAL_MS)
    public void cancelExpiredReservations(){
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(PAYMENT_TIMEOUT_MINUTES);

        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndReservationTimeBefore(ReservationStatus.PENDING, cutoffTime);

        for (Reservation reservation : expiredReservations) {
            Long reservationId = reservation.getId();

            try {
                if (reservationService.cancelReservationIfPending(reservationId)){
                    log.info("Anulowano przeterminowaną rezerwację ID: {}. Miejsca zostały zwolnione.", reservationId);
                }
            } catch (ObjectOptimisticLockingFailureException e) {
                // the user paid at the very same moment - the payment won, so we leave the reservation alone
                log.info("Rezerwacja ID: {} została zmieniona w międzyczasie (prawdopodobnie opłacona) - pomijam anulowanie.", reservationId);
            }
        }
    }

}

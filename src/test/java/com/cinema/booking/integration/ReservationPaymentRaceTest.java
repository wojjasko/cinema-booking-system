package com.cinema.booking.integration;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.ReservationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The race between "user pays" vs "scheduler cancels expired reservation" on the real database
// thanks to @Version - only one sites win, never both suddenly and never and "quiet" override
class ReservationPaymentRaceTest extends IntegrationTestBase {

    private static final int ROUNDS = 25;

    @Test
    void paymentAndCancellationRacingEachOther_exactlyOneWins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < ROUNDS; round++) {
                Reservation reservation = reserve(seats.get(0), OWNER);
                Long id = reservation.getId();
                CyclicBarrier startTogether = new CyclicBarrier(2);

                Future<Boolean> payment = pool.submit(() -> {
                    startTogether.await();
                    try {
                        return reservationService.processBlikPayment(id, "123456", OWNER);
                    } catch (IllegalStateException e) {
                        return false;   // "time for the payment has expired" - lose of payment
                    }
                });

                Future<Boolean> cancellation = pool.submit(() -> {
                    startTogether.await();
                    try {
                        return reservationService.cancelReservationIfPending(id);
                    } catch (ObjectOptimisticLockingFailureException e) {
                        return false;   // payment was first - scheduler leaves
                    }
                });

                boolean paid = payment.get(30, TimeUnit.SECONDS);
                boolean cancelled = cancellation.get(30, TimeUnit.SECONDS);

                // exaclty one side win (XOR)
                assertThat(paid ^ cancelled)
                        .as("runda %d: platnosc=%s, anulowanie=%s - wygrac musi dokladnie jedna strona", round, paid, cancelled)
                        .isTrue();

                // the database winner need to be correct
                Reservation saved = reservationRepository.findById(id).orElseThrow();
                long ticketCount = ticketRepository.findByReservationScreeningId(screening.getId()).stream()
                        .filter(ticket -> ticket.getReservation().getId().equals(id))
                        .count();

                if (paid) {
                    assertThat(saved.getStatus()).isEqualTo(ReservationStatus.PAID);
                    assertThat(ticketCount).as("oplacona rezerwacja zachowuje bilety").isEqualTo(1);
                } else {
                    assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
                    assertThat(ticketCount).as("anulowana rezerwacja zwalnia miejsca").isZero();
                }

                // cleaning round, so that the same place can be reserved in the next one
                reservationRepository.deleteById(id);
                reservationIds.remove(id);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void stalePaymentIsRejectedAfterReservationWasCancelled() {
        Reservation reservation = reserve(seats.get(0), OWNER);

        // "outdated copy" of reservation with the version before cancelling (thats how the payment sees it)
        Reservation staleCopy = reservationRepository.findById(reservation.getId()).orElseThrow();

        // the scheduler cancels the reservations and confirms the change (the version in the database increments)
        assertThat(reservationService.cancelReservationIfPending(reservation.getId())).isTrue();

        // an attempt to save an "already paid for" but outdated copy must fail; it must not overwrite the cancellation
        staleCopy.setStatus(ReservationStatus.PAID);
        assertThatThrownBy(() -> reservationRepository.saveAndFlush(staleCopy))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);
    }
}

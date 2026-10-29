package com.cinema.booking.integration;

import com.cinema.booking.model.Reservation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

//an evidence, that screening blockade (SELECT ... FOR UPDATE) works in the real PostgreSQL
//many threads at the same time try to reserve the same seat, but only one can do it

class ReservationConcurrencyTest extends IntegrationTestBase {

    private static final int THREADS = 6;

    @Test
    void onlyOneOfManyParallelReservationsGetsTheSameSeat() throws Exception {
        Long seatId = seats.get(0).getId();
        Long screeningId = screening.getId();

        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch allReady = new CountDownLatch(THREADS);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Reservation>> futures = new ArrayList<>();

            for (int i = 0; i < THREADS; i++) {
                String email = "klient" + i + "@test.pl";
                futures.add(pool.submit(() -> {
                    allReady.countDown();
                    start.await();   // all threads start at the same time
                    return reservationService.createReservation(screeningId, List.of(seatId), email);
                }));
            }

            allReady.await();
            start.countDown();

            int succeeded = 0;
            int rejected = 0;
            for (Future<Reservation> future : futures) {
                try {
                    Reservation reservation = future.get(30, TimeUnit.SECONDS);
                    reservationIds.add(reservation.getId());
                    succeeded++;
                } catch (ExecutionException e) {
                    // those who couldnt reserve need to receive a message with taken place, not any other error or sth
                    assertThat(e.getCause())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("zajęte");
                    rejected++;
                }
            }

            assertThat(succeeded).as("liczba udanych rezerwacji tego samego miejsca").isEqualTo(1);
            assertThat(rejected).isEqualTo(THREADS - 1);
            assertThat(ticketRepository.findOccupiedSeatIdsByScreeningId(screeningId)).containsExactly(seatId);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void parallelReservationsOfDifferentSeatsAllSucceed() throws Exception {
        Long screeningId = screening.getId();

        ExecutorService pool = Executors.newFixedThreadPool(seats.size());
        try {
            CountDownLatch allReady = new CountDownLatch(seats.size());
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Reservation>> futures = new ArrayList<>();

            for (int i = 0; i < seats.size(); i++) {
                Long seatId = seats.get(i).getId();
                String email = "klient" + i + "@test.pl";
                futures.add(pool.submit(() -> {
                    allReady.countDown();
                    start.await();
                    return reservationService.createReservation(screeningId, List.of(seatId), email);
                }));
            }

            allReady.await();
            start.countDown();

            for (Future<Reservation> future : futures) {
                reservationIds.add(future.get(30, TimeUnit.SECONDS).getId());
            }

            // the blockade can't block the reservation of any various seats - all 4 need to be done
            assertThat(ticketRepository.findOccupiedSeatIdsByScreeningId(screeningId)).hasSize(seats.size());
        } finally {
            pool.shutdownNow();
        }
    }
}

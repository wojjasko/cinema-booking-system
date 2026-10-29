package com.cinema.booking.service;

import com.cinema.booking.model.*;
import com.cinema.booking.repository.ReservationRepository;
import com.cinema.booking.repository.SeatRepository;
import com.cinema.booking.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    private static final Long SCREENING_ID = 1L;
    private static final Long HALL_ID = 10L;
    private static final String OWNER = "jan@kino.pl";

    @Mock private ReservationRepository reservationRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private SeatRepository seatRepository;
    @Mock private ScreeningService screeningService;

    @InjectMocks private ReservationService reservationService;

    private CinemaHall hall;
    private Screening screening;

    @BeforeEach
    void setUp() {
        hall = CinemaHall.builder().id(HALL_ID).name("Sala 1").totalRows(5).seatsPerRow(5).build();
        screening = Screening.builder()
                .id(SCREENING_ID)
                .cinemaHall(hall)
                .price(new BigDecimal("30.00"))
                .build();
    }

    private Seat seat(Long id, CinemaHall seatHall) {
        return Seat.builder().id(id).rowNumber(1).seatNumber(id.intValue()).cinemaHall(seatHall).build();
    }

    private Reservation reservation(Long id, String email, ReservationStatus status) {
        return Reservation.builder()
                .id(id)
                .screening(screening)
                .customerEmail(email)
                .totalPrice(new BigDecimal("30.00"))
                .status(status)
                .tickets(new ArrayList<>())
                .build();
    }

    // ---------- creating a reservation ----------

    @Test
    void createReservation_savesPendingReservationWithCorrectTotalAndTickets() {
        when(screeningService.getScreeningByIdForUpdate(SCREENING_ID)).thenReturn(screening);
        when(seatRepository.findAllById(any())).thenReturn(List.of(seat(1L, hall), seat(2L, hall)));
        when(ticketRepository.findOccupiedSeatIdsByScreeningId(SCREENING_ID)).thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        Reservation result = reservationService.createReservation(SCREENING_ID, List.of(1L, 2L), OWNER);

        assertThat(result.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(result.getCustomerEmail()).isEqualTo(OWNER);
        assertThat(result.getTotalPrice()).isEqualByComparingTo("60.00");
        assertThat(result.getTickets()).hasSize(2);
        assertThat(result.getTickets()).allSatisfy(t -> assertThat(t.getReservation()).isSameAs(result));
    }

    @Test
    void createReservation_locksScreeningRowToPreventDoubleBooking() {
        when(screeningService.getScreeningByIdForUpdate(SCREENING_ID)).thenReturn(screening);
        when(seatRepository.findAllById(any())).thenReturn(List.of(seat(1L, hall)));
        when(ticketRepository.findOccupiedSeatIdsByScreeningId(SCREENING_ID)).thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        reservationService.createReservation(SCREENING_ID, List.of(1L), OWNER);

        // the version without blockade cant be used in reservation
        verify(screeningService).getScreeningByIdForUpdate(SCREENING_ID);
        verify(screeningService, never()).getScreeningById(any());
    }

    @Test
    void createReservation_rejectsSeatThatIsAlreadyTaken() {
        when(screeningService.getScreeningByIdForUpdate(SCREENING_ID)).thenReturn(screening);
        when(seatRepository.findAllById(any())).thenReturn(List.of(seat(1L, hall), seat(2L, hall)));
        when(ticketRepository.findOccupiedSeatIdsByScreeningId(SCREENING_ID)).thenReturn(List.of(2L));

        assertThatThrownBy(() -> reservationService.createReservation(SCREENING_ID, List.of(1L, 2L), OWNER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("zajęte");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_rejectsSeatFromDifferentHall() {
        CinemaHall otherHall = CinemaHall.builder().id(99L).name("Sala 2").build();
        when(screeningService.getScreeningByIdForUpdate(SCREENING_ID)).thenReturn(screening);
        when(seatRepository.findAllById(any())).thenReturn(List.of(seat(1L, otherHall)));

        assertThatThrownBy(() -> reservationService.createReservation(SCREENING_ID, List.of(1L), OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nie należy do sali");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_rejectsNonExistentSeat() {
        when(screeningService.getScreeningByIdForUpdate(SCREENING_ID)).thenReturn(screening);
        // two seats expected, the database knows only one
        when(seatRepository.findAllById(any())).thenReturn(List.of(seat(1L, hall)));

        assertThatThrownBy(() -> reservationService.createReservation(SCREENING_ID, List.of(1L, 999L), OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nieistniejące");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void createReservation_countsRepeatedSeatIdOnlyOnce() {
        when(screeningService.getScreeningByIdForUpdate(SCREENING_ID)).thenReturn(screening);
        when(seatRepository.findAllById(any())).thenReturn(List.of(seat(1L, hall)));
        when(ticketRepository.findOccupiedSeatIdsByScreeningId(SCREENING_ID)).thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(inv -> inv.getArgument(0));

        // the same seat sent twice (for example hand-modified form) = one ticket
        Reservation result = reservationService.createReservation(SCREENING_ID, List.of(1L, 1L), OWNER);

        assertThat(result.getTickets()).hasSize(1);
        assertThat(result.getTotalPrice()).isEqualByComparingTo("30.00");
    }

    // ---------- reservation owner ----------

    @Test
    void getReservationForUser_returnsReservationToItsOwner_ignoringEmailCase() {
        Reservation reservation = reservation(5L, OWNER, ReservationStatus.PENDING);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThat(reservationService.getReservationForUser(5L, "JAN@kino.pl")).isSameAs(reservation);
    }

    @Test
    void getReservationForUser_deniesAccessToSomeoneElsesReservation() {
        when(reservationRepository.findById(5L))
                .thenReturn(Optional.of(reservation(5L, OWNER, ReservationStatus.PAID)));

        assertThatThrownBy(() -> reservationService.getReservationForUser(5L, "obcy@kino.pl"))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- BLIk payment ----------

    @Test
    void processBlikPayment_marksReservationAsPaid_forValidCode() {
        Reservation reservation = reservation(5L, OWNER, ReservationStatus.PENDING);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        boolean result = reservationService.processBlikPayment(5L, "123456", OWNER);

        assertThat(result).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PAID);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void processBlikPayment_returnsFalseAndKeepsPending_forInvalidCode() {
        Reservation reservation = reservation(5L, OWNER, ReservationStatus.PENDING);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThat(reservationService.processBlikPayment(5L, "12ab56", OWNER)).isFalse();
        assertThat(reservationService.processBlikPayment(5L, "12345", OWNER)).isFalse();
        assertThat(reservationService.processBlikPayment(5L, null, OWNER)).isFalse();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void processBlikPayment_rejectsReservationThatIsNotPending() {
        when(reservationRepository.findById(5L))
                .thenReturn(Optional.of(reservation(5L, OWNER, ReservationStatus.CANCELLED)));

        assertThatThrownBy(() -> reservationService.processBlikPayment(5L, "123456", OWNER))
                .isInstanceOf(IllegalStateException.class);

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void processBlikPayment_deniesPayingForSomeoneElsesReservation() {
        Reservation reservation = reservation(5L, OWNER, ReservationStatus.PENDING);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.processBlikPayment(5L, "123456", "obcy@kino.pl"))
                .isInstanceOf(AccessDeniedException.class);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void processBlikPayment_reportsExpiredReservation_whenSomeoneChangedItInTheMeantime() {
        Reservation reservation = reservation(5L, OWNER, ReservationStatus.PENDING);
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));
        //simulation: in the meantime the scheduler cancelled the reservation, so saving with the old version fails
        when(reservationRepository.saveAndFlush(reservation))
                .thenThrow(new ObjectOptimisticLockingFailureException(Reservation.class, 5L));

        assertThatThrownBy(() -> reservationService.processBlikPayment(5L, "123456", OWNER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Czas na płatność minął");
    }

    // ---------- cancelling an expired reservation ----------

    @Test
    void cancelReservationIfPending_cancelsPendingReservationAndFreesItsSeats() {
        Reservation reservation = reservation(5L, OWNER, ReservationStatus.PENDING);
        reservation.getTickets().add(Ticket.builder().reservation(reservation).seat(seat(1L, hall)).build());
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        boolean cancelled = reservationService.cancelReservationIfPending(5L);

        assertThat(cancelled).isTrue();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getTickets()).isEmpty();
        verify(reservationRepository).saveAndFlush(reservation);
    }

    @Test
    void cancelReservationIfPending_doesNotTouchAlreadyPaidReservation() {
        Reservation reservation = reservation(5L, OWNER, ReservationStatus.PAID);
        reservation.getTickets().add(Ticket.builder().reservation(reservation).seat(seat(1L, hall)).build());
        when(reservationRepository.findById(5L)).thenReturn(Optional.of(reservation));

        assertThat(reservationService.cancelReservationIfPending(5L)).isFalse();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PAID);
        assertThat(reservation.getTickets()).hasSize(1);
        verify(reservationRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelReservationIfPending_returnsFalseForMissingReservation() {
        when(reservationRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(reservationService.cancelReservationIfPending(404L)).isFalse();

        verify(reservationRepository, never()).saveAndFlush(any());
    }
}

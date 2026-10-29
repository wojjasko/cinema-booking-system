package com.cinema.booking.integration;

import com.cinema.booking.model.Reservation;
import com.cinema.booking.model.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

// HTTP security tests: who can access which addresses. Queries go through real filter chain
// Spring Security (springSecurity()), "logged user" simulated by user("email").
class ReservationAccessControlTest extends IntegrationTestBase {

    private static final String STRANGER = "obcy@test.pl";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ---------- not logged ----------

    @Test
    void unauthenticatedUser_isRedirectedToLogin_onPaymentPage() throws Exception {
        Reservation reservation = reserve(seats.get(0), OWNER);

        mockMvc.perform(get("/payment/{id}", reservation.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).endsWith("/login"));
    }

    @Test
    void unauthenticatedUser_isRedirectedToLogin_andCannotReserveSeats() throws Exception {
        mockMvc.perform(post("/reserve")
                        .with(csrf())
                        .param("screeningId", screening.getId().toString())
                        .param("seatIds", seats.get(0).getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).endsWith("/login"));

        // not a singl reservation was created
        assertThat(ticketRepository.findOccupiedSeatIdsByScreeningId(screening.getId())).isEmpty();
    }

    @Test
    void unauthenticatedUser_isRedirectedToLogin_onMyReservations() throws Exception {
        mockMvc.perform(get("/my-reservations"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).endsWith("/login"));
    }

    // ---------- admin panel (regression following a change to the order of rules in SecurityConfig) ----------

    @Test
    void adminPanel_isNotAvailableForUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertThat(result.getResponse().getRedirectedUrl()).endsWith("/login"));
    }

    @Test
    void adminPanel_isForbiddenForRegularUser() throws Exception {
        mockMvc.perform(get("/admin").with(user(OWNER).roles("USER")))
                .andExpect(status().isForbidden());
    }

    // ---------- stranger user ----------

    @Test
    void stranger_gets403_onSomeoneElsesPaymentPage() throws Exception {
        Reservation reservation = reserve(seats.get(0), OWNER);

        mockMvc.perform(get("/payment/{id}", reservation.getId()).with(user(STRANGER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void stranger_gets403_whenPayingForSomeoneElsesReservation_andItStaysPending() throws Exception {
        Reservation reservation = reserve(seats.get(0), OWNER);

        mockMvc.perform(post("/payment/{id}/pay", reservation.getId())
                        .with(user(STRANGER))
                        .with(csrf())
                        .param("blikCode", "123456"))
                .andExpect(status().isForbidden());

        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void stranger_gets403_onSomeoneElsesTicketPdf() throws Exception {
        Reservation reservation = reserve(seats.get(0), OWNER);
        reservationService.processBlikPayment(reservation.getId(), "123456", OWNER);

        mockMvc.perform(get("/reservations/{id}/pdf", reservation.getId()).with(user(STRANGER)))
                .andExpect(status().isForbidden());
    }

    // ---------- owner ----------

    @Test
    void owner_seesOwnPaymentPage() throws Exception {
        Reservation reservation = reserve(seats.get(0), OWNER);

        mockMvc.perform(get("/payment/{id}", reservation.getId()).with(user(OWNER)))
                .andExpect(status().isOk())
                .andExpect(view().name("payment"));
    }

    @Test
    void owner_canDownloadOwnTicketPdf_afterPaying() throws Exception {
        Reservation reservation = reserve(seats.get(0), OWNER);
        reservationService.processBlikPayment(reservation.getId(), "123456", OWNER);

        byte[] pdf = mockMvc.perform(get("/reservations/{id}/pdf", reservation.getId()).with(user(OWNER)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();

        // each file starts with "%PDF"
        assertThat(new String(pdf, 0, 4)).isEqualTo("%PDF");
    }
}

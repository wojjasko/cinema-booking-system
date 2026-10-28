package com.cinema.booking.model;

//represents the whole order from client (reservation date, status and total amount)

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_id", nullable = false)
    private Screening screening;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private LocalDateTime reservationTime;

    @Column(nullable = false)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Ticket> tickets = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.PENDING;

    //row version - protects against two simultaneous changes of the same reservation
    //for examplle: the user pays exactly when the scheduler cancels it). The second change fails with
    //ObjectOptimisticLockingFailureException instead of silently overwriting the first one.
    //"default 0" fills the new column in reservations that already exist in the database.
    @Version
    @Column(nullable = false, columnDefinition = "bigint not null default 0")
    private Long version;
}

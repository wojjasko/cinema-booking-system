# 🎬 Cinema Booking Management System

A Full-stack web application for online cinema ticket booking, interactive seat selection, BLIK payments, PDF ticket generation, and advanced administration of screenings, movies, and reservations.

---

## Key Features

### Ticket Booking & Public Area
* **Movie Screening Aggregation:** Displaying a single tile per movie featuring a poster, duration, genre, along with date and time buttons.
* **Interactive Seat Selection:** A visual cinema hall layout with real-time marking of available and occupied seats.
* **My Reservations:** A client panel listing booked tickets with an option to download confirmation in PDF format.
* **Strict Concurrency Control:** Anti-double-booking protection preventing simultaneous users from reserving the exact same seat on a screening.

### Admin Panel & Hall Management
- **Repertoire Management (CRUD):** Adding and deleting movies, managing cinema halls, and scheduling new screenings.
- **Overlap Prevention Algorithm:** SQL-based validation ensuring a screening hall is not double-booked during a specific time slot (factoring in movie runtime).
- **Admin Hall Preview:** Visual overview of occupied seats featuring CSS Tooltips with client data (email, reservation date/time, transaction ID).
- **Flexible Cancellation:** Administrative ability to cancel individual tickets or entire user bookings for specific screenings with JPA cascading cleanup (`orphanRemoval`).

### Security & Registration
- **Role-Based Access Control:** Spring Security integration strictly isolating `ROLE_USER` and `ROLE_ADMIN` routes.
- **Strict Reservation Ownership:** Ownership checks (`AccessDeniedException` / HTTP 403) guaranteeing users can only view, pay for, or download PDF tickets belonging to their own account.
- **Advanced Registration Validation:** 
  - International dialing code selection (e.g., +48, +44, +49).
  - Automatic input blocking for non-numeric characters in the phone field.
  - Real-time auto-capitalization of first and last names with restriction on digits and special characters.
- **Frontend Modularization:** Reusable Thymeleaf fragments (`fragments/navbar.html`) paired with isolated CSS modules (`navbar.css`).
---

## 🛠️ Tech Stack

- **Backend:** Java 21, Spring Boot (Spring Security, Spring Data JPA, Hibernate)
- **Database:** PostgreSQL
- **Frontend:** Thymeleaf, HTML5, CSS3 (Glassmorphism Dark Theme), JavaScript (ES6)
- **Testing:** JUnit 5, Mockito, Spring Boot Test Starter
- **Build Tool & Utilities:** Maven, Lombok
- **CI/CD:** GitHub Actions CI Pipeline
- **Concurrency & Locking:** Pessimistic Write Locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`)
- **Document & Media Generation:** iText PDF 7, ZXing (QR Code generation)

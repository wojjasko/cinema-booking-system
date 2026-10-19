# 🎬 Cinema Booking Management System

A Full-stack web application for online cinema ticket booking, interactive seat selection, and advanced management of screenings, movies, and reservations via an admin panel.

---

## Key Features

### Ticket Booking & Public Area
* **Movie Screening Aggregation:** Displaying a single tile per movie featuring a poster, duration, genre, along with date and time buttons.
* **Interactive Seat Selection:** A visual cinema hall layout with real-time marking of available and occupied seats.
* **My Reservations:** A client panel listing booked tickets with an option to download confirmation in PDF format.

### Admin Panel & Hall Management
- **Repertoire Management (CRUD):** Adding and removing movies, plus scheduling new screenings.
- **Overlap Prevention Algorithm:** SQL-based validation ensuring a screening hall is not double-booked during a specific time slot (factoring in movie duration).
- **Admin Hall Preview:** Visual overview of occupied seats featuring CSS Tooltips with client data (email, reservation date/time, transaction ID).
- **Flexible Cancellation:** Ability to cancel specific client reservations for any screening.

### Security & Registration
- **Role-Based Access Control:** Separation of user privileges into client (`ROLE_USER`) and administrator (`ROLE_ADMIN`) accounts via Spring Security.
- **Advanced Registration Validation:** 
  - International dialing code selection (e.g., +48, +44, +49).
  - Automatic input blocking for non-numeric characters in the phone field.
  - Real-time auto-capitalization of first and last names with restriction on digits and special characters.

---

## 🛠️ Tech Stack

- **Backend:** Java 17, Spring Boot (Spring Security, Spring Data JPA)
- **Database:** PostgreSQL
- **Frontend:** Thymeleaf, HTML5, CSS3 (Glassmorphism, Dark Theme), JavaScript (ES6)
- **Build Tool:** Maven
- **Libraries:** Lombok

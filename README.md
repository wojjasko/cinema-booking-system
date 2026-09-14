# 🎬 Cinema Booking Management System

Aplikacja webowa Full-stack służąca do rezerwacji biletów kinowych online, interaktywnego wyboru miejsc na sali oraz zaawansowanego zarządzania seansami, filmami i rezerwacjami z poziomu panelu administratora.

---

## 🌟 Kluczowe Funkcjonalności

### 🎟️ Strona Główna & Rezerwacja Biletów
* **Agregacja seansów per film:** Wyświetlanie pojedynczego kafelka dla danego filmu z plakatem, czasem trwania i gatunkiem oraz przyciskami dat i godzin seansów.
* **Interaktywny wybór miejsc:** Wizualny plan sali kinowej z dynamicznym oznaczaniem miejsc wolnych i zajętych w czasie rzeczywistym.
* **Moje Rezerwacje:** Panel klienta ze spisem zarezerwowanych biletów oraz możliwością pobrania potwierdzenia w formacie PDF.

### ⚙️ Panel Administratora & Wgląd w Salę
* **Zarządzanie Repertuarem (CRUD):** Dodawanie i usuwanie filmów oraz planowanie nowych seansów.
* **Algorytm weryfikacji nakładania seansów:** Bezpieczne sprawdzanie w bazie danych SQL, czy wybrana sala kinowa nie jest już zajęta w danym przedziale czasowym (uwzględnia czas trwania filmu).
* **Interaktywny podgląd sali dla Admina:** Możliwość podglądu zajętych foteli z dymkami (Tooltip CSS) zawierającymi dane klienta (e-mail, data i czas rezerwacji, ID transakcji).
* **Elastyczne anulowanie rezerwacji:** Możliwość anulowania rezerwacji miejsca/miejsc danego klienta dla każdego seansu.

### 🔒 Bezpieczeństwo i Rejestracja
* **Role-Based Access Control:** Podział uprawnień użytkowników na konta klienta (`ROLE_USER`) oraz administratora (`ROLE_ADMIN`) z użyciem Spring Security.
* **Zaawansowana walidacja rejestracji:** 
  * Wybór międzynarodowych kodów kierunkowych (np. 🇵🇱 +48, 🇬🇧 +44, 🇩🇪 +49).
  * Automatyczna blokada wprowadzania znaków niebędących cyframi w polu telefonu.
  * Auto-kapitalizacja pierwszej litery imienia/nazwiska z blokadą cyfr i znaków specjalnych w czasie rzeczywistym.

---

## 🛠️ Stack Techniczny

* **Backend:** Java 17, Spring Boot (Spring Security, Spring Data JPA)
* **Database:** PostgreSQL
* **Frontend:** Thymeleaf, HTML5, CSS3 (Glassmorphism, Dark Theme), JavaScript (ES6)
* **Build Tool:** Maven
* **Libraries:** Lombok

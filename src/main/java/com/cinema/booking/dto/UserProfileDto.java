package com.cinema.booking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileDto {
    @NotBlank(message = "Imię jest wymagane")
    private String firstName;

    @NotBlank(message = "Nazwisko jest wymagane")
    private String lastName;

    @NotBlank(message = "Email jest wymagany")
    @Email(message = "Niepoprawny format adresu e-mail")
    private String email;

    private String phoneNumber;
}

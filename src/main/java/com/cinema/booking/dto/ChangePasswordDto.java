package com.cinema.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordDto {

    @NotBlank(message = "Aktualne hasło jest wymagane")
    private String currentPassword;

    @NotBlank(message = "Nowe hasło jest wymagane")
    private String newPassword;
}

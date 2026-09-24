package com.cinema.booking.service;

import com.cinema.booking.dto.ChangePasswordDto;
import com.cinema.booking.dto.UserProfileDto;
import com.cinema.booking.model.Role;
import com.cinema.booking.model.User;
import com.cinema.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getUserByEmail(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono użytkownika o e-mailu" + email));
    }

    public User registerNewUser(String email, String rawPassword, String firstName, String lastName, String phoneNumber){
        //sprawdzenie czy email jest zajety przez innego uzytkownika
        if(userRepository.existsByEmail(email)){
            throw new RuntimeException("Konto o podanym adresie e-mail już istnieje!");
        }

        //szyfrujemy surowe haslo wpisane w formularzu za pomoca BCrypt
        String encodedPassword = passwordEncoder.encode(rawPassword);

        //budujemy nowy obiekt uzytkownika z rola ROLE_USER
        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .role(Role.ROLE_USER)
                .build();

        //zapisujemy uzytkownika w bazie danych
        return userRepository.save(user);
    }

    @Transactional
    public void updateUserProfile(String currentEmail, UserProfileDto dto){
        User user = getUserByEmail(currentEmail);

        if(!user.getEmail().equals(dto.getEmail()) && userRepository.existsByEmail(dto.getEmail())){
            throw new IllegalArgumentException("Ten adres e-mail już istnieje");
        }

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPhoneNumber(dto.getPhoneNumber());

        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String currentEmail, ChangePasswordDto dto){
        User user = getUserByEmail(currentEmail);

        if(!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())){
            throw new IllegalArgumentException("Aktualne hasło jest nieprawidłowe");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

}

package com.cinema.booking.service;

import com.cinema.booking.model.Role;
import com.cinema.booking.model.User;
import com.cinema.booking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}

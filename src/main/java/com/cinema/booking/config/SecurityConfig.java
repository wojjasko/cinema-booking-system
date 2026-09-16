package com.cinema.booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    //szyfrowanie hasel algorytmem BCrypt
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    //lancuch filtrow chroniacy sciezki w aplikacji
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                    //sciezki widoczne dla kazdego
                    .requestMatchers("/", "/*", "/register", "/login",
                            "/css/**", "/js/**", "/images/**", "/screenings/**",
                            "/webjars/**").permitAll()
                    //dostep tylko dla adminow
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    //kazda innna sciezka wymaga logowania
                    .anyRequest().authenticated()
                )
                //konfiguracja formularza logowania
                .formLogin(form -> form
                    .loginPage("/login")
                    .defaultSuccessUrl("/", false)
                    .permitAll()
                )
                .sessionManagement(session -> session
                    .maximumSessions(1)
                )
                //konfiguracja wylogowania
                .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true) //czyscimy sesje przy wylogowaniu
                    .deleteCookies("JSESSIONID")
                    .permitAll()
                );

        return http.build();
    }
}

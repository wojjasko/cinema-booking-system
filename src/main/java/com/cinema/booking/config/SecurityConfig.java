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

    //passwords encryption with BCrypt Algorithm
    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    //chain of filters that protects specific paths in applications
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/admin/**").hasRole("ADMIN")
                    .requestMatchers("/profile", "/my-reservations").authenticated()
                    .requestMatchers("/", "/*", "/register", "/login",
                                "/css/**", "/js/**", "/images/**", "/screenings/**",
                                "/webjars/**").permitAll()
                    .anyRequest().authenticated()
                )
                //login form configuration
                .formLogin(form -> form
                    .loginPage("/login")
                    .defaultSuccessUrl("/", false)
                    .permitAll()
                )
                .sessionManagement(session -> session
                    .maximumSessions(1)
                )
                //logout configuration
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

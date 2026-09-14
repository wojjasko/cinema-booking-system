package com.cinema.booking.controller;


import com.cinema.booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    //1. formularz logowania
    @GetMapping("/login")
    public String showLoginForm(){
        return "login";
    }

    //2. formularz rejestracji
    @GetMapping("/register")
    public String showRegisterForm(){
        return "register";
    }

    //3. obsluga wyslanego formularza rejestracji
    @PostMapping("/register")
    public String processRegister(@RequestParam String email,
                                  @RequestParam String password,
                                  @RequestParam String firstName,
                                  @RequestParam String lastName,
                                  @RequestParam String countryCode,
                                  @RequestParam String phoneNumber,
                                  Model model){
        try{
            String fullPhoneNumber = countryCode + phoneNumber.trim();

            if (!firstName.matches("^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźżA-ZĄĆĘŁŃÓŚŹŻa-z\\s-]*$")) {
                throw new IllegalArgumentException("Imię musi zaczynać się od wielkiej litery i nie może zawierać cyfr!");
            }
            if (!lastName.matches("^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźżA-ZĄĆĘŁŃÓŚŹŻa-z\\s-]*$")) {
                throw new IllegalArgumentException("Nazwisko musi zaczynać się od wielkiej litery i nie może zawierać cyfr!");
            }

           userService.registerNewUser(email, password, firstName, lastName, phoneNumber);
           return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }
}

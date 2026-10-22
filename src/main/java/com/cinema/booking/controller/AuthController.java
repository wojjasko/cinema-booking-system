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

    //name/surname: big letter at the start, later only letters, spaces and dash
    private static final String NAME_PATTERN = "^[A-ZĄĆĘŁŃÓŚŹŻ][a-ząćęłńóśźżA-ZĄĆĘŁŃÓŚŹŻa-z\\s-]*$";

    private final UserService userService;

    //1. login form
    @GetMapping("/login")
    public String showLoginForm(){
        return "login";
    }

    //2. register form
    @GetMapping("/register")
    public String showRegisterForm(){
        return "register";
    }

    //3. service of sent register form
    @PostMapping("/register")
    public String processRegister(@RequestParam String email,
                                  @RequestParam String password,
                                  @RequestParam String firstName,
                                  @RequestParam String lastName,
                                  @RequestParam String countryCode,
                                  @RequestParam String phoneNumber,
                                  Model model){
        try{
            if (!firstName.matches(NAME_PATTERN)) {
                throw new IllegalArgumentException("Imię musi zaczynać się od wielkiej litery i nie może zawierać cyfr!");
            }
            if (!lastName.matches(NAME_PATTERN)) {
                throw new IllegalArgumentException("Nazwisko musi zaczynać się od wielkiej litery i nie może zawierać cyfr!");
            }

            // saving the number with countryCode
            String fullPhoneNumber = countryCode + phoneNumber.trim();

           userService.registerNewUser(email, password, firstName, lastName, fullPhoneNumber);
           return "redirect:/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "register";
        }
    }
}

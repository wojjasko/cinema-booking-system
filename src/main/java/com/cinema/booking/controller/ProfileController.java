package com.cinema.booking.controller;

import com.cinema.booking.dto.ChangePasswordDto;
import com.cinema.booking.dto.UserProfileDto;
import com.cinema.booking.model.User;
import com.cinema.booking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model){
        User user = userService.getUserByEmail(userDetails.getUsername());

        // dane dto wypelnia dane formularza
        UserProfileDto profileDto = new UserProfileDto();
        profileDto.setFirstName(user.getFirstName());
        profileDto.setLastName(user.getLastName());
        profileDto.setEmail(user.getEmail());
        profileDto.setPhoneNumber(user.getPhoneNumber());

        model.addAttribute("user", user);
        model.addAttribute("profileDto", profileDto);
        model.addAttribute("passwordDto", new ChangePasswordDto());

        return "profile";
    }

    @PostMapping("/update-details")
    public String updateDetails(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute UserProfileDto dto,
                                RedirectAttributes redirectAttributes){

        try {
            userService.updateUserProfile(userDetails.getUsername(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Dane osobowe zostały pomyślnie zaktualizowane");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/change-password")
    public String changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                 @ModelAttribute ChangePasswordDto dto,
                                 RedirectAttributes redirectAttributes) {
        try {
            userService.changePassword(userDetails.getUsername(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Hasło zostało pomyślnie zmienione");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }
}
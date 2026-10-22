package com.cinema.booking.controller;

import com.cinema.booking.dto.ChangePasswordDto;
import com.cinema.booking.dto.UserProfileDto;
import com.cinema.booking.model.User;
import com.cinema.booking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// DTO validation annotations (@NotBlank, @Email) need @Valid to work
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    @GetMapping
    public String showProfile(@AuthenticationPrincipal UserDetails userDetails, Model model){
        User user = userService.getUserByEmail(userDetails.getUsername());

        // dto data fullfill the form
        UserProfileDto profileDto = new UserProfileDto();
        profileDto.setFirstName(user.getFirstName());
        profileDto.setLastName(user.getLastName());
        profileDto.setEmail(user.getEmail());
        profileDto.setPhoneNumber(user.getPhoneNumber());

        model.addAttribute("profileDto", profileDto);
        model.addAttribute("passwordDto", new ChangePasswordDto());

        return "profile";
    }

    @PostMapping("/update-details")
    public String updateDetails(@AuthenticationPrincipal UserDetails userDetails,
                                @Valid @ModelAttribute UserProfileDto dto,
                                BindingResult bindingResult,
                                RedirectAttributes redirectAttributes){

        if(bindingResult.hasErrors()){
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/profile";
        }

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
                                 @Valid @ModelAttribute ChangePasswordDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if(bindingResult.hasErrors()){
            redirectAttributes.addFlashAttribute("errorMessage", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/profile";
        }

        try {
            userService.changePassword(userDetails.getUsername(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Hasło zostało pomyślnie zmienione");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }
}

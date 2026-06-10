package hr.algebra.photoapp.controller;

import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.service.UserActionService;
import hr.algebra.photoapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

// Controller Pattern (MVC)
// Handles user profile and settings requests
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final UserActionService userActionService;

    @GetMapping
    public String profile(Authentication authentication, Model model) {
        User user = getLoggedUser(authentication);
        
        if (user == null) {
            System.err.println("❌ User is null in profile controller!");
            return "redirect:/login?error=notAuthenticated";
        }
        
        String username = user.getUsername();
        Map<String, Object> stats = userService.getUserStatistics(username);

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("packages", PackageType.values());
        model.addAttribute("canChangePackage", userService.canChangePackage(username));
        model.addAttribute("recentActions", userActionService.getRecentActions(username, 10));

        return "profile";
    }

    @PostMapping("/change-package")
    public String changePackage(@RequestParam PackageType packageType,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User user = getLoggedUser(authentication);
        
        if (user == null) {
            return "redirect:/login";
        }
        
        String username = user.getUsername();

        try {
            if (!userService.canChangePackage(username)) {
                redirectAttributes.addFlashAttribute("error",
                        "❌ You can only change package once per day");
            } else {
                userService.requestPackageChange(username, packageType);
                redirectAttributes.addFlashAttribute("success",
                        "✅ Package change requested. It will take effect tomorrow.");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "❌ " + e.getMessage());
        }

        return "redirect:/profile";
    }

    @GetMapping("/statistics")
    public String statistics(Authentication authentication, Model model) {
        User user = getLoggedUser(authentication);

        if (user == null) {
            return "redirect:/login";
        }

        String username = user.getUsername();
        model.addAttribute("stats", userService.getUserStatistics(username));
        model.addAttribute("actions", userActionService.getUserActions(username));

        return "user-statistics";
    }

    private User getLoggedUser(Authentication authentication) {
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getName().equals("anonymousUser")) {
            System.err.println("❌ Authentication is null or anonymous");
            return null;
        }

        String username = authentication.getName();
        System.out.println("✅ Getting user for username: " + username);
        
        User user = userService.findByUsername(username);
        
        if (user == null) {
            System.err.println("❌ User not found for username: " + username);
        } else {
            System.out.println("✅ User found: " + user.getUsername() + " (id: " + user.getId() + ")");
        }
        
        return user;
    }
}

package hr.algebra.photoapp.controller.admin;

import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.util.FunctionalPhotoHelpers;
import hr.algebra.photoapp.service.PhotoService;
import hr.algebra.photoapp.service.UserActionService;
import hr.algebra.photoapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

// Controller Pattern (MVC)
// Handles admin-specific requests for user management
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final PhotoService photoService;
    private final UserActionService userActionService;

    @GetMapping
    public String adminDashboard(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        model.addAttribute("photos", photoService.getLatestPhotos(10));
        return "admin/dashboard";
    }

    @GetMapping("/users/{id}")
    public String userDetails(@PathVariable Long id, Model model) {
        User user = userService.findById(id);
        Map<String, Object> stats = userService.getUserStatistics(user.getUsername());
        
        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("packages", PackageType.values());
        model.addAttribute("actions", userActionService.getUserActions(user.getUsername()));
        
        return "admin/user-details";
    }

    @PostMapping("/users/{id}/update")
    public String updateUser(@PathVariable Long id,
                            @RequestParam(required = false) String email,
                            @RequestParam(required = false) PackageType packageType,
                            RedirectAttributes redirectAttributes) {
        
        try {
            userService.updateUserProfile(id, email, packageType);
            redirectAttributes.addFlashAttribute("success", "User updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", " " + e.getMessage());
        }
        
        return "redirect:/admin/users/" + id;
    }

    @GetMapping("/actions")
    public String viewActions(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<hr.algebra.photoapp.model.UserAction> actionPage = 
                userActionService.getAllActions(page, 50);
        
        model.addAttribute("actions", actionPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", actionPage.getTotalPages());
        
        return "admin/actions";
    }

    @GetMapping("/statistics")
    public String systemStatistics(Model model) {
        var recentActions = userActionService.getAllActions(0, 200).getContent();
        var latestPhotos = photoService.getLatestPhotos(10);

        model.addAttribute("actionCounts", FunctionalPhotoHelpers.countActionsByType(recentActions));
        model.addAttribute("latestPhotosTotalBytes", FunctionalPhotoHelpers.totalPhotoBytes(latestPhotos));

        return "admin/statistics";
    }
}

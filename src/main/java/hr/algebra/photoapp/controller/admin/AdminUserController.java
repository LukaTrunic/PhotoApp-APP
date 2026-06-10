package hr.algebra.photoapp.controller.admin;

import hr.algebra.photoapp.model.PackageType;
import hr.algebra.photoapp.model.User;
import hr.algebra.photoapp.repository.UserRepository;
import hr.algebra.photoapp.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public AdminUserController(UserRepository userRepository,
                               UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    // List of users
    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("packages", PackageType.values());
        return "admin/users";
    }

    // Admin can change package
    @PostMapping("/package")
    public String changePackage(@RequestParam Long userId,
                                @RequestParam PackageType packageType) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        userService.changePackage(user.getUsername(), packageType);

        return "redirect:/admin/users";
    }

    // Admin can delete user
    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {

        try {
            userService.deleteUserByAdmin(id);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", " " + e.getMessage());
        }

        return "redirect:/admin";
    }

}


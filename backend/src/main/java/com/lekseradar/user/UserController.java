package com.lekseradar.user;

import com.lekseradar.auth.CurrentUser;
import com.lekseradar.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    public UserController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public record UpdateProfileRequest(
            @Size(min = 1, max = 100) String displayName,
            String currentPassword,
            @Size(min = 8, max = 100, message = "Пароль должен быть от 8 символов") String newPassword) {}

    @GetMapping("/me")
    public UserDto me(@CurrentUser User user) {
        return UserDto.from(user);
    }

    @PutMapping("/me")
    @Transactional
    public UserDto update(@CurrentUser User current, @Valid @RequestBody UpdateProfileRequest req) {
        User user = users.findById(current.getId()).orElseThrow(() -> ApiException.notFound("Пользователь"));
        if (req.displayName() != null && !req.displayName().isBlank()) {
            user.setDisplayName(req.displayName().trim());
        }
        if (req.newPassword() != null) {
            if (req.currentPassword() == null || !encoder.matches(req.currentPassword(), user.getPasswordHash())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Текущий пароль указан неверно");
            }
            user.setPasswordHash(encoder.encode(req.newPassword()));
        }
        return UserDto.from(user);
    }
}

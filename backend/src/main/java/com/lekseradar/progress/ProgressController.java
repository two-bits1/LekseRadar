package com.lekseradar.progress;

import com.lekseradar.auth.CurrentUser;
import com.lekseradar.user.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {
    private final ProgressService service;

    public ProgressController(ProgressService service) {
        this.service = service;
    }

    /** @param tz часовой пояс пользователя, например Europe/Moscow (для подсчёта дней и серии). */
    @GetMapping
    public ProgressDto get(@CurrentUser User user, @RequestParam(required = false) String tz) {
        ZoneId zone;
        try {
            zone = tz == null || tz.isBlank() ? ZoneOffset.UTC : ZoneId.of(tz);
        } catch (DateTimeException e) {
            zone = ZoneOffset.UTC;
        }
        return service.forUser(user.getId(), zone);
    }
}

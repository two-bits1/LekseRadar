package com.lekseradar.controller;

import com.lekseradar.dto.dashboard.DashboardResponseDTO;
import com.lekseradar.dto.dashboard.EventResponseDTO;
import com.lekseradar.model.UserEntity;
import com.lekseradar.security.CurrentUser;
import com.lekseradar.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "7. Дашборд и история", description = "Сводка для главной и прогресса, лента действий")
public class DashboardController {
    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Сводка: ДЗ, квизы, карточки, темы, активность по дням")
    public DashboardResponseDTO dashboard(@CurrentUser UserEntity user) {
        return dashboard.dashboard(user.getId());
    }

    @GetMapping("/history")
    @Operation(summary = "Что я делал: новые сверху", description = "limit 1–200, по умолчанию 50")
    public List<EventResponseDTO> history(@CurrentUser UserEntity user, @RequestParam(defaultValue = "50") int limit) {
        return dashboard.history(user.getId(), limit);
    }
}

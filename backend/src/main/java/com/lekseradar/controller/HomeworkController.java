package com.lekseradar.controller;

import com.lekseradar.dto.homework.AssignmentResponseDTO;
import com.lekseradar.dto.homework.UpdateAssignmentRequestDTO;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.model.UserEntity;
import com.lekseradar.model.enums.AssignmentStatusEnum;
import com.lekseradar.security.CurrentUser;
import com.lekseradar.dto.homework.SyncResponseDTO;
import com.lekseradar.service.HomeworkService;
import com.lekseradar.service.HomeworkSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/homework")
@Tag(name = "6. Домашка", description = "ДЗ со всех платформ (собирает homework-collector)")
public class HomeworkController {
    private final HomeworkService homework;
    private final HomeworkSyncService sync;

    public HomeworkController(HomeworkService homework, HomeworkSyncService sync) {
        this.homework = homework;
        this.sync = sync;
    }

    @GetMapping
    @Operation(summary = "Мои ДЗ", description = "Сначала ближайший дедлайн. status: todo | overdue | submitted | graded")
    public List<AssignmentResponseDTO> list(@CurrentUser UserEntity user,
                                            @RequestParam(required = false) String status) {
        AssignmentStatusEnum filter = null;
        if (status != null && !status.isBlank()) {
            try {
                filter = AssignmentStatusEnum.fromValue(status.strip().toLowerCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("status: todo | overdue | submitted | graded");
            }
        }
        return homework.list(user.getId(), filter);
    }

    @PostMapping("/sync")
    @Operation(summary = "Обновить ДЗ со всех платформ",
            description = "Не чаще раза в минуту (429 + Retry-After). Нет подключений → пустой sources")
    public SyncResponseDTO sync(@CurrentUser UserEntity user) {
        return sync.sync(user.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Одно ДЗ с полным текстом задания")
    public AssignmentResponseDTO get(@CurrentUser UserEntity user, @PathVariable Long id) {
        return homework.get(user.getId(), id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Отметить сданным или вернуть в «надо сделать»", description = "status: submitted | todo")
    public AssignmentResponseDTO update(@CurrentUser UserEntity user, @PathVariable Long id,
                                        @Valid @RequestBody UpdateAssignmentRequestDTO req) {
        return homework.setStatus(user.getId(), id, req.status());
    }
}

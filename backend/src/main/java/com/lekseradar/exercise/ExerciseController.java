package com.lekseradar.exercise;

import com.lekseradar.auth.CurrentUser;
import com.lekseradar.exercise.ExerciseDtos.*;
import com.lekseradar.user.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercises")
public class ExerciseController {
    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ExerciseDto> generate(@CurrentUser User user, @Valid @RequestBody GenerateRequest req) {
        return service.generate(user.getId(), req);
    }

    @GetMapping
    public PageDto<ExerciseDto> list(@CurrentUser User user,
                                     @RequestParam(required = false) String subject,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size) {
        return service.list(user.getId(), subject, page, size);
    }

    @GetMapping("/subjects")
    public List<String> subjects(@CurrentUser User user) {
        return service.subjects(user.getId());
    }

    @GetMapping("/{id}")
    public ExerciseDto get(@CurrentUser User user, @PathVariable Long id) {
        return service.get(user.getId(), id);
    }

    @PostMapping("/{id}/answer")
    public ExerciseDto answer(@CurrentUser User user, @PathVariable Long id, @Valid @RequestBody AnswerRequest req) {
        return service.answer(user.getId(), id, req.answer().strip());
    }
}

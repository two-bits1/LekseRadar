package com.lekseradar.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lekseradar.dto.platform.*;
import com.lekseradar.exception.BadRequestException;
import com.lekseradar.model.UserEntity;
import com.lekseradar.model.enums.PlatformEnum;
import com.lekseradar.security.CurrentUser;
import com.lekseradar.service.PlatformService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/platforms")
@Tag(name = "8. Платформы", description = "Подключение Canvas, Teams, itslearning, InSchool. "
        + "Ошибки: 400 неверные данные или адрес, 422 платформа не приняла доступ, 429 слишком часто, "
        + "502 платформа недоступна, 503 collector недоступен. 401 не бывает")
public class PlatformController {
    private final PlatformService platforms;

    public PlatformController(PlatformService platforms) {
        this.platforms = platforms;
    }

    @GetMapping
    @Operation(summary = "Мои платформы", description = "Всегда 4 строки: canvas, teams, itslearning, inschool. Без токенов")
    public List<PlatformResponseDTO> list(@CurrentUser UserEntity user) {
        return platforms.list(user.getId());
    }

    @PostMapping("/{platform}")
    @Operation(summary = "Подключить платформу",
            description = "Тело — доступы в формате homework-collector (snake_case). Сервер проверяет их одним сбором ДЗ, "
                    + "сохраняет зашифрованными и сразу записывает задания. Повторный вызов обновляет подключение. "
                    + "Пустые строки считаются «не указано»")
    public PlatformResponseDTO connect(
            @CurrentUser UserEntity user,
            @PathVariable String platform,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json",
                    schema = @Schema(type = "object"), examples = {
                    @ExampleObject(name = "canvas", value = "{\"base_url\": \"https://uio.instructure.com\", \"token\": \"7~abc\"}"),
                    @ExampleObject(name = "inschool", value = "{\"host\": \"ohg-vgs.inschool.visma.no\", \"cookies\": {\"JSESSIONID\": \"…\", \"Authorization\": \"…\"}}"),
                    @ExampleObject(name = "teams", value = "{\"access_token\": \"eyJ…\", \"refresh_token\": \"0.A…\"}"),
                    @ExampleObject(name = "itslearning", value = "{\"base_url\": \"https://sms.itslearning.com\", \"refresh_token\": \"…\"}"),
                    @ExampleObject(name = "itslearning (код после входа)", value = "{\"base_url\": \"https://sms.itslearning.com\", \"code_or_url\": \"itsl-itslearning://login?code=…\"}")}))
            @RequestBody JsonNode body) {
        return platforms.connect(user.getId(), parse(platform), body);
    }

    @PostMapping("/teams/start")
    @Operation(summary = "Teams: вход по коду (по желанию)",
            description = "Вместо ручного ввода токенов: показать ученику userCode и ссылку verificationUri, "
                    + "потом опрашивать /teams/poll раз в interval секунд")
    public TeamsStartResponseDTO teamsStart(@CurrentUser UserEntity user) {
        return platforms.teamsStart(user.getId());
    }

    @PostMapping("/teams/poll")
    @Operation(summary = "Teams: вошёл ли ученик", description = "{\"status\":\"pending\"} или {\"status\":\"connected\",\"platform\":{…}}")
    public TeamsPollResponseDTO teamsPoll(@CurrentUser UserEntity user, @Valid @RequestBody TeamsPollRequestDTO req) {
        return platforms.teamsPoll(user.getId(), req.deviceCode());
    }

    @GetMapping("/itslearning/url")
    @Operation(summary = "itslearning: ссылка для входа (по желанию)",
            description = "После входа ученик вставляет код или всю ссылку в POST /platforms/itslearning как code_or_url")
    public UrlResponseDTO itslearningUrl(@RequestParam String baseUrl) {
        return new UrlResponseDTO(platforms.itslearningUrl(baseUrl));
    }

    @DeleteMapping("/{platform}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Отключить платформу", description = "Доступ удаляется, ДЗ этой платформы остаются")
    public void disconnect(@CurrentUser UserEntity user, @PathVariable String platform) {
        platforms.disconnect(user.getId(), parse(platform));
    }

    private static PlatformEnum parse(String platform) {
        try {
            return PlatformEnum.fromValue(platform.strip().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("platform: canvas | teams | itslearning | inschool");
        }
    }
}

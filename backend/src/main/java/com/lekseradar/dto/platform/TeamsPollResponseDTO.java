package com.lekseradar.dto.platform;

import com.fasterxml.jackson.annotation.JsonInclude;

/** {@code {"status": "pending"}} — ученик ещё не ввёл код; {@code {"status": "connected", "platform": {…}}} — готово. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TeamsPollResponseDTO(String status, PlatformResponseDTO platform) {

    public static TeamsPollResponseDTO pending() {
        return new TeamsPollResponseDTO("pending", null);
    }

    public static TeamsPollResponseDTO connected(PlatformResponseDTO platform) {
        return new TeamsPollResponseDTO("connected", platform);
    }
}

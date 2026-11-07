package com.lekseradar.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Меняются только переданные поля.
 *
 * @param locale   язык интерфейса и тьютора: nb, nn, en, ru
 * @param timezone часовой пояс, например Europe/Oslo
 */
public record UpdateProfileRequest(
        @Size(min = 1, max = 100) String displayName,
        @Pattern(regexp = "nb|nn|en|ru", message = "Допустимо: nb, nn, en, ru") String locale,
        @Size(max = 64) String timezone) {}

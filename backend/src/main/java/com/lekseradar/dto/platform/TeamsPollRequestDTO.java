package com.lekseradar.dto.platform;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeamsPollRequestDTO(@NotBlank @Size(max = 4000) String deviceCode) {}

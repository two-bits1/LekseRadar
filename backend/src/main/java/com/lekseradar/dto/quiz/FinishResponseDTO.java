package com.lekseradar.dto.quiz;

import java.math.BigDecimal;

/** @param score балл 0–100 (partial засчитывается как 0,5) */
public record FinishResponseDTO(BigDecimal score) {}

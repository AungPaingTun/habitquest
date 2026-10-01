package com.habitquest.prize.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrizeCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 16) String icon,
        @NotNull @Min(1) @Max(100000) Integer cost
) {
    public PrizeCreateRequest {
        name = name == null ? null : name.trim();
        icon = icon == null || icon.isBlank() ? null : icon.trim();
    }
}

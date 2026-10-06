package com.habitquest.prize.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrizeCreateRequest(
        @NotBlank(message = "Give your prize a name") @Size(max = 100, message = "Name can be at most 100 characters") String name,
        @Size(max = 16, message = "Pick a single emoji as the icon") String icon,
        @NotNull(message = "Set a cost in points")
        @Min(value = 1, message = "Cost must be between 1 and 100,000 points")
        @Max(value = 100000, message = "Cost must be between 1 and 100,000 points") Integer cost
) {
    public PrizeCreateRequest {
        name = name == null ? null : name.trim();
        icon = icon == null || icon.isBlank() ? null : icon.trim();
    }
}

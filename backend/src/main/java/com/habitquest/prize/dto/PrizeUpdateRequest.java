package com.habitquest.prize.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Only name and icon can be edited. There's no cost field: a prize's cost is locked once created (PRD §5.5). */
public record PrizeUpdateRequest(
        @NotBlank(message = "Give your prize a name") @Size(max = 100, message = "Name can be at most 100 characters") String name,
        @Size(max = 16, message = "Pick a single emoji as the icon") String icon
) {
    public PrizeUpdateRequest {
        name = name == null ? null : name.trim();
        icon = icon == null || icon.isBlank() ? null : icon.trim();
    }
}

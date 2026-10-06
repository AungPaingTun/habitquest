package com.habitquest.common;

import com.habitquest.auth.dto.RegisterRequest;
import com.habitquest.habit.dto.HabitRequest;
import com.habitquest.prize.dto.PrizeCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/** The frontend shows these messages under form fields as-is, so they must read as friendly sentences. */
class ValidationMessagesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void habitRequest_blankNameAndTooManyPoints_givesFriendlyMessages() {
        var errors = errorsFor(new HabitRequest(" ", null, 500, null, null, null, null));

        assertThat(errors).containsEntry("name", "Give your habit a name")
                .containsEntry("points", "Points must be between 1 and 100");
    }

    @Test
    void prizeCreateRequest_missingCost_givesFriendlyMessage() {
        var errors = errorsFor(new PrizeCreateRequest("Hotpot", null, null));

        assertThat(errors).containsOnly(Map.entry("cost", "Set a cost in points"));
    }

    @Test
    void registerRequest_badEmailAndShortPassword_givesFriendlyMessages() {
        var errors = errorsFor(new RegisterRequest("not-an-email", "short", "Aung", null));

        assertThat(errors).containsEntry("email", "Enter a valid email address")
                .containsEntry("password", "Password must be 8 to 72 characters");
    }

    private Map<String, String> errorsFor(Object request) {
        return validator.validate(request).stream()
                .collect(Collectors.toMap(v -> v.getPropertyPath().toString(), ConstraintViolation::getMessage, (a, b) -> a));
    }
}

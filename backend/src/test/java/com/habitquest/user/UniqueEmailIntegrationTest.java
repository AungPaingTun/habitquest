package com.habitquest.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the database enforces the unique-email constraint (not just the application). Saving a second user with
 * an email already taken must be rejected by PostgreSQL, so two accounts can never share an email even if two
 * sign-ups race past the application's own check.
 */
@SpringBootTest
class UniqueEmailIntegrationTest {

    @Autowired private UserRepository users;

    @Test
    void duplicateEmail_isRejectedByTheDatabase() {
        String email = "dupe-" + System.nanoTime() + "@test.local";
        users.saveAndFlush(new User(email, "hash", "First", "UTC"));

        assertThatThrownBy(() -> users.saveAndFlush(new User(email, "hash", "Second", "UTC")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

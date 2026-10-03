package com.eadalat.auth.seed;

import com.eadalat.auth.entity.Role;
import com.eadalat.auth.entity.User;
import com.eadalat.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the default ADMIN account on first startup if it does not exist.
 */
@Component
public class AdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private static final String ADMIN_EMAIL = "admin@eadalat.local";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAdmin() {
        if (users.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        User admin = User.builder()
                .name("Administrator")
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .build();
        users.save(admin);
        log.info("Seeded default ADMIN account: {}", ADMIN_EMAIL);
    }
}

package ch.bbw.pr.tresorbackend.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Separate config so BCryptPasswordEncoder has no circular dependency with SecurityConfig.
 */
@Configuration
public class EncoderConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

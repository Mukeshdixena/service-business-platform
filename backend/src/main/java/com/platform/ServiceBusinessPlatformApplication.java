package com.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * UserDetailsServiceAutoConfiguration is excluded because authentication in
 * this application is entirely JWT/BCrypt-based (see AuthService, JwtService,
 * JwtAuthenticationFilter) — there is no Spring Security UserDetailsService or
 * AuthenticationManager in play, so leaving it enabled would only generate a
 * random "default user" password on every startup for a feature we don't use.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ServiceBusinessPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceBusinessPlatformApplication.class, args);
    }
}

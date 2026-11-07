package org.musicpracticepal.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${spring.security.user.password:password}")
    private String defaultAdminPassword;
    @Value("${spring.security.user.name:admin}")
    private String defaultAdminUsername;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.POST, "/song-charts/*/upload").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/songs", "/songs/*/remove").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/songs/*/links").hasRole("ADMIN")
                .anyRequest().permitAll());
        http.formLogin(form -> form
                .loginPage("/")
                .loginProcessingUrl("/login")
                .failureUrl("/?loginError")
                .defaultSuccessUrl("/", true)
                .permitAll());
        http.logout(logout -> logout.logoutSuccessUrl("/"));
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, exception) ->
                        response.sendRedirect("/?loginRequired")));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(User.withUsername(defaultAdminUsername)
                .password(passwordEncoder.encode(defaultAdminPassword))
                .roles("ADMIN")
                .build());
    }
}

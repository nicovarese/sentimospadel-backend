package com.sentimospadel.backend.config.security;

import com.sentimospadel.backend.notification.config.NotificationPushProperties;
import com.sentimospadel.backend.match.config.MatchInvitationProperties;
import com.sentimospadel.backend.player.config.PlayerProfilePhotoStorageProperties;
import com.sentimospadel.backend.tournament.config.TournamentInvitationProperties;
import com.sentimospadel.backend.user.entity.User;
import com.sentimospadel.backend.user.repository.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, NotificationPushProperties.class, MatchInvitationProperties.class, TournamentInvitationProperties.class, PlayerProfilePhotoStorageProperties.class})
public class SecurityConfig {

    private static final List<String> LOCAL_DEV_ORIGIN_PATTERNS = List.of(
            "http://localhost",
            "https://localhost",
            "http://127.0.0.1",
            "https://127.0.0.1",
            "http://localhost:*",
            "https://localhost:*",
            "http://127.0.0.1:*",
            "https://127.0.0.1:*",
            "capacitor://localhost",
            "ionic://localhost"
    );

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        // Keep the API stateless and only protect the routes that already depend on the JWT-backed user context.
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/health", "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/verify-email", "/api/auth/verify-email/resend").permitAll()
                        .requestMatchers("/api/auth/me", "/api/account/**", "/api/onboarding/**", "/api/players/me", "/api/players/me/**", "/api/notifications", "/api/notifications/**", "/api/clubs/me/management/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/matches", "/api/matches/*/join", "/api/matches/*/leave", "/api/matches/*/cancel", "/api/matches/*/teams", "/api/matches/*/invite-link", "/api/matches/*/result", "/api/matches/*/result/confirm", "/api/matches/*/result/reject").authenticated()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/tournaments",
                                "/api/tournaments/*/join",
                                "/api/tournaments/*/leave",
                                "/api/tournaments/*/launch-preview",
                                "/api/tournaments/*/launch",
                                "/api/tournaments/*/invite-link",
                                "/api/tournaments/*/archive",
                                "/api/tournaments/*/matches/*/result",
                                "/api/tournaments/*/matches/*/result/confirm",
                                "/api/tournaments/*/matches/*/result/reject"
                        ).authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/tournaments/*/entries", "/api/tournaments/*/entries/me/team-name").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .anonymous(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(LOCAL_DEV_ORIGIN_PATTERNS);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager(DaoAuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder);
        return authenticationProvider;
    }

    @Bean
    UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> userRepository.findByEmail(normalizeEmail(username))
                .map(this::toUserDetails)
                .orElseThrow(() -> new UsernameNotFoundException("User with email " + username + " was not found"));
    }

    private UserDetails toUserDetails(User user) {
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                .disabled(user.getStatus() != com.sentimospadel.backend.user.enums.UserStatus.ACTIVE)
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}

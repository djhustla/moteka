package main.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // ============ PUBLIC ENDPOINTS ============
                        // Pages HTML et ressources statiques
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/favicon.ico",
                                "/error"
                        ).permitAll()

                        // Inscription et connexion
                        .requestMatchers(
                                "/api/users/register",
                                "/api/db/**",
                                "/api/acces/**","/api/users/validate-account",

                                "/api/users/register-admin"
                        ).permitAll()

                        // Authentification
                        .requestMatchers("/api/auth/**").permitAll()

                        // Pages publiques
                        .requestMatchers(
                                "/inscription/**",

                                "/infrabel/**",

                                ////////////////////////
                                "/stripe/**",
                                "/create-payment-intent",
                                "/create-payment-intent",

                                "/create-subscription",


                                /////////////////////////////
                                "/connection/**",
                                "/menu/**",
                                "/encoder_mix.html",
                                "/les_derniers_lives.html",
                                "/le_dernier_live.html",
                                "/genre.html",
                                "/ajouter_genre_musical.html"
                        ).permitAll()


                        // Email via Mailjet - AJOUTEZ CETTE LIGNE
                        .requestMatchers("/api/email/**").permitAll()



                        .requestMatchers(
                                "/api/music-favoris/users/me"
                        ).permitAll()
                        // API publiques
                        .requestMatchers(
                                "/api/users/all",
                                "/api/users/search_contains",
                                "/api/conversion/**",
                                "/api/github/**",
                                "/api/dropbox/liens",
                                "/api/music-genres/**",
                                "/api/playlists/**"
                        ).permitAll()

                        // Ressources statiques
                        .requestMatchers(
                                "/images/**",
                                "/data/**",
                                "/playlist/**",
                                "/textfiles/**",
                                "/photos/**"
                        ).permitAll()

                        // H2 Console (dev seulement - à retirer en production)
                        .requestMatchers("/h2-console/**").permitAll()

                        // ============ AUTHENTICATED ENDPOINTS ============
                        // ENDPOINTS CRITIQUES : /api/users/me/** - DOIVENT ÊTRE EN PREMIER !
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/me/**").authenticated() // TOUS les sous-endpoints

                        // Matching musical
                        .requestMatchers(
                                "/api/users/perfect-music-matches",
                                "/api/users/music-soulmates",
                                "/api/users/music-enemies",
                                "/api/users/me/same-music-favoris",
                                "/api/users/me/opposite-music-favoris",
                                "/api/users/me/same-top-music"
                        ).authenticated()

                        // Favoris musicaux
                        .requestMatchers("/api/users/me/music-favoris/**").authenticated()
                        .requestMatchers("/api/users/*/music-favoris").authenticated()

                        // Administration
                        .requestMatchers("/api/users/admin/**").hasRole("ADMIN")

                        // RÈGLE GÉNÉRALE POUR /api/users/ - IMPORTANT : APRÈS les règles spécifiques
                        .requestMatchers("/api/users/**").authenticated()

                        // Messages et conversations
                        .requestMatchers(
                                "/api/conversations/**",
                                "/api/messages/**"
                        ).authenticated()

                        // ============ FALLBACK ============
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "https://motekk.onrender.com",
                                "https://security-learning-1.onrender.com",
                                "http://localhost:3000",
                                "http://localhost:8080",
                                "http://127.0.0.1:8080"
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                        .allowedHeaders("*")
                        .exposedHeaders("Authorization") // Important pour le front-end
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
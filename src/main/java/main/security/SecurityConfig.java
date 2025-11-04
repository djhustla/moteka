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

                        .requestMatchers("/api/users/register").permitAll()
                        .requestMatchers("/api/users/register-admin").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/index.html").permitAll()
                        .requestMatchers("/inscription/**").permitAll()
                        .requestMatchers("/connection/**").permitAll()

                        // ✅ AJOUTER CES DEUX LIGNES
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/error").permitAll()  // ← IMPORTANT !



                        .requestMatchers("/").permitAll()
                        .requestMatchers("/menu/**").permitAll()
                        .requestMatchers("/data/**").permitAll()

                        .requestMatchers("/playlist/**").permitAll()

                        .requestMatchers("/textfiles/**").permitAll()
                        .requestMatchers("/photos/**").permitAll()
                        .requestMatchers("/api/users/all").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        .requestMatchers("/api/music-genres/**").permitAll()
                        .requestMatchers("/api/playlists/**").permitAll()  // ✅ CORRIGÉ - avec /**

                        // Autoriser l'accès au fichier HTML
                        .requestMatchers("/genre.html").permitAll()
                        .requestMatchers("/ajouter_genre_musical.html").permitAll()

                        // NOUVEAUX ENDPOINTS POUR LE MATCHING
                        .requestMatchers("/api/users/perfect-music-matches").authenticated()
                        .requestMatchers("/api/users/music-soulmates").authenticated()
                        .requestMatchers("/api/users/music-enemies").authenticated()

                        // Endpoints existants
                        .requestMatchers("/api/conversations/**").authenticated()
                        .requestMatchers("/api/users/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/messages/**").authenticated()
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
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}
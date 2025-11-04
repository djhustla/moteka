package main.controlleurs;

import ch.qos.logback.core.boolex.Matcher;
import main.modeles.User;
import main.security.JwtUtil;
import main.services.TokenService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService; // Service des utilisateurs

    @Autowired
    private JwtUtil jwtUtil; // Utilitaire JWT

    @Autowired
    private   PasswordEncoder passwordEncoder;


    @Autowired
    private TokenService tokenService; // ✅ utilise la BDD


    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        return userService.findByUsername(username)
                .map(user -> {
                    if (userService.validatePassword(password, user.getPassword())) {
                        String accessToken = jwtUtil.generateAccessToken(username);
                        String refreshToken = jwtUtil.generateRefreshToken(username);

                        Map<String, String> response = new HashMap<>();
                        response.put("message", "Connexion réussie");
                        response.put("username", username);
                        response.put("accessToken", accessToken);
                        response.put("refreshToken", refreshToken);
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.badRequest().body(Map.of("error", "Mot de passe incorrect"));
                    }
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "Utilisateur non trouvé")));
    }

    // ✅ REFRESH
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> tokenData) {
        String refreshToken = tokenData.get("refreshToken");

        try {
            // Vérifie en BDD
            if (tokenService.isRefreshTokenInvalidated(refreshToken)) {
                return ResponseEntity.status(401).body(Map.of("error", "Refresh token invalide (logout effectué)"));
            }

            String username = jwtUtil.extractUsername(refreshToken);
            if (jwtUtil.validateToken(refreshToken, username)) {
                String newAccessToken = jwtUtil.generateAccessToken(username);
                return ResponseEntity.ok(Map.of(
                        "accessToken", newAccessToken,
                        "refreshToken", refreshToken
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh token invalide ou expiré"));
        }

        return ResponseEntity.status(401).body(Map.of("error", "Erreur de validation"));
    }

    // ✅ LOGOUT (invalide en BDD)
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> tokenData) {
        String refreshToken = tokenData.get("refreshToken");

        tokenService.invalidateRefreshToken(refreshToken);
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie. Refresh token invalidé."));
    }

    // Endpoint pour valider un JWT
    @PostMapping("/validate")
    public ResponseEntity<Map<String, String>> validateToken(@RequestBody Map<String, String> tokenData) {
        String token = tokenData.get("token");

        try {
            String username = jwtUtil.extractUsername(token);

            if (jwtUtil.validateToken(token, username)) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Token valide");
                response.put("username", username);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            // Si le token est invalide ou expiré
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Token invalide ou expiré"));
    }


}

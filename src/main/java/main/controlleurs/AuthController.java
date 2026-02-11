package main.controlleurs;

import main.modeles.User;
import main.security.JwtUtil;
import main.services.TokenService;
import main.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenService tokenService;

    // ✅ LOGIN AVEC VÉRIFICATION DE LA VALIDATION SMS
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        try {
            String identifier = credentials.get("username"); // Peut être username, email ou phone
            String password = credentials.get("password");

            // Validation des champs obligatoires
            if (identifier == null || identifier.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Identifiant (username, email ou téléphone) requis"));
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Mot de passe requis"));
            }

            // Chercher l'utilisateur
            return userService.findByUsernameOrEmail(identifier)
                    .map(user -> {
                        // === NOUVEAU : Vérifier si le compte est validé ===
                        if (!user.getIsActive()) {
                            return ResponseEntity.status(403)
                                    .body(Map.of(
                                            "error", "Compte non validé",
                                            "message", "Veuillez valider votre compte par SMS avant de vous connecter",
                                            "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "",
                                            "canResendCode", "true"
                                    ));
                        }

                        // Vérifier le mot de passe
                        if (userService.validatePassword(password, user.getPassword())) {
                            String accessToken = jwtUtil.generateAccessToken(user.getUsername());
                            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

                            Map<String, String> response = new HashMap<>();
                            response.put("message", "Connexion réussie");
                            response.put("username", user.getUsername());
                            response.put("email", user.getEmail());
                            response.put("phoneNumber", user.getPhoneNumber());
                            response.put("role", user.getRole());
                            response.put("isActive", user.getIsActive().toString());
                            response.put("accessToken", accessToken);
                            response.put("refreshToken", refreshToken);
                            return ResponseEntity.ok(response);
                        } else {
                            return ResponseEntity.status(401)
                                    .body(Map.of("error", "Mot de passe incorrect"));
                        }
                    })
                    .orElse(ResponseEntity.status(401)
                            .body(Map.of("error", "Utilisateur non trouvé")));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur interne du serveur: " + e.getMessage()));
        }
    }

    // ✅ ENDPOINT POUR VÉRIFIER L'ÉTAT DU COMPTE AVANT CONNEXION
    @PostMapping("/check-account")
    public ResponseEntity<Map<String, Object>> checkAccount(@RequestBody Map<String, String> data) {
        try {
            String identifier = data.get("identifier");

            if (identifier == null || identifier.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Identifiant requis"));
            }

            return userService.findByUsernameOrEmail(identifier)
                    .map(user -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("exists", true);
                        response.put("isActive", user.getIsActive());
                        response.put("username", user.getUsername());
                        response.put("email", user.getEmail());
                        response.put("phoneNumber", user.getPhoneNumber());

                        if (!user.getIsActive()) {
                            response.put("message", "Compte en attente de validation par SMS");
                            response.put("canResendCode", true);
                        } else {
                            response.put("message", "Compte validé et prêt pour la connexion");
                        }

                        return ResponseEntity.ok(response);
                    })
                    .orElse(ResponseEntity.ok(Map.of(
                            "success", true,
                            "exists", false,
                            "message", "Aucun compte trouvé avec cet identifiant"
                    )));

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "error", "Erreur serveur: " + e.getMessage()));
        }
    }

    // ✅ REFRESH (inchangé)
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, String>> refresh(@RequestBody Map<String, String> tokenData) {
        String refreshToken = tokenData.get("refreshToken");

        try {
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

    // ✅ LOGOUT (inchangé)
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestBody Map<String, String> tokenData) {
        String refreshToken = tokenData.get("refreshToken");
        tokenService.invalidateRefreshToken(refreshToken);
        return ResponseEntity.ok(Map.of("message", "Déconnexion réussie. Refresh token invalidé."));
    }

    // ✅ VALIDATE TOKEN (inchangé)
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
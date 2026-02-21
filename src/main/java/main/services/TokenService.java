package main.services;

import main.modeles.InvalidatedToken;
import main.repository.InvalidatedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    @Autowired
    private InvalidatedTokenRepository invalidatedTokenRepository;

    // ✅ Enregistrer un token invalidé en BDD
    public void invalidateRefreshToken(String token) {
        InvalidatedToken entity = new InvalidatedToken(token);
        invalidatedTokenRepository.save(entity);
    }

    // ✅ Vérifier en BDD si token est invalidé
    public boolean isRefreshTokenInvalidated(String token) {
        return invalidatedTokenRepository.findByToken(token).isPresent();
    }
}

package ch.bbw.pr.tresorbackend.service;

import ch.bbw.pr.tresorbackend.model.PasswordResetToken;
import ch.bbw.pr.tresorbackend.model.User;
import ch.bbw.pr.tresorbackend.repository.PasswordResetTokenRepository;
import ch.bbw.pr.tresorbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles forgot-password flow: token generation, email sending, token validation, password reset.
 *
 * Security note: After a password reset the user's existing secrets can no longer be decrypted,
 * because the AES key is derived from the old password. The user is warned about this on the UI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JavaMailSender mailSender;
    private final PasswordEncryptService passwordEncryptService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.reset-token-expiry-minutes:60}")
    private int expiryMinutes;

    /** Minimum minutes between two reset emails for the same address (rate limiting). */
    private static final int MIN_MINUTES_BETWEEN_EMAILS = 5;

    public enum RequestResult { SENT, USER_NOT_FOUND, RATE_LIMITED }

    public RequestResult requestPasswordReset(String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            log.warn("PasswordResetService: no user found for email {}", email);
            return RequestResult.USER_NOT_FOUND;
        }

        // Rate limiting: block if an unexpired token was sent recently
        Optional<PasswordResetToken> existing = tokenRepository.findByEmail(email);
        if (existing.isPresent()) {
            LocalDateTime minAllowed = existing.get().getCreatedAt().plusMinutes(MIN_MINUTES_BETWEEN_EMAILS);
            if (LocalDateTime.now().isBefore(minAllowed)) {
                log.warn("PasswordResetService: rate limit hit for email {}", email);
                return RequestResult.RATE_LIMITED;
            }
            tokenRepository.deleteByEmail(email);
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(
                null, token, email,
                LocalDateTime.now().plusMinutes(expiryMinutes),
                LocalDateTime.now()
        );
        tokenRepository.save(resetToken);

        sendResetEmail(email, user.getFirstName(), token);
        log.info("PasswordResetService: reset email sent to {}", email);
        return RequestResult.SENT;
    }

    public enum ResetResult { SUCCESS, TOKEN_NOT_FOUND, TOKEN_EXPIRED }

    public ResetResult resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> optToken = tokenRepository.findByToken(token);
        if (optToken.isEmpty()) {
            log.warn("PasswordResetService: token not found: {}", token);
            return ResetResult.TOKEN_NOT_FOUND;
        }
        PasswordResetToken resetToken = optToken.get();
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenRepository.delete(resetToken);
            log.warn("PasswordResetService: token expired for email {}", resetToken.getEmail());
            return ResetResult.TOKEN_EXPIRED;
        }

        User user = userService.findByEmail(resetToken.getEmail());
        if (user != null) {
            user.setPassword(passwordEncryptService.hashPassword(newPassword));
            userRepository.save(user);
            log.info("PasswordResetService: password reset for {}", resetToken.getEmail());
        }

        tokenRepository.delete(resetToken);
        return ResetResult.SUCCESS;
    }

    /** Cleanup job: delete all expired tokens every hour. */
    @Scheduled(fixedRate = 3_600_000)
    public void cleanupExpiredTokens() {
        tokenRepository.deleteAllExpired(LocalDateTime.now());
        log.info("PasswordResetService: expired tokens cleaned up");
    }

    private void sendResetEmail(String to, String firstName, String token) {
        String link = baseUrl + "/user/reset-password?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("TresorApp – Passwort zurücksetzen");
        message.setText(
                "Hallo " + firstName + ",\n\n" +
                "Sie haben angefordert, Ihr Passwort zurückzusetzen.\n\n" +
                "Klicken Sie auf folgenden Link (gültig für " + expiryMinutes + " Minuten):\n" +
                link + "\n\n" +
                "WICHTIG: Nach dem Zurücksetzen sind Ihre gespeicherten Secrets nicht mehr " +
                "entschlüsselbar, da der Schlüssel aus Ihrem alten Passwort abgeleitet wurde.\n\n" +
                "Falls Sie diese E-Mail nicht angefordert haben, ignorieren Sie sie bitte.\n\n" +
                "TresorApp"
        );
        mailSender.send(message);
    }
}

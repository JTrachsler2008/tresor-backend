package ch.bbw.pr.tresorbackend.service;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static dev.samstevens.totp.util.Utils.getDataUriForImage;

/**
 * TOTP-based two-factor authentication (compatible with Google Authenticator).
 */
@Slf4j
@Service
public class TwoFactorService {

    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    public String generateQrCodeDataUri(String email, String secret) {
        QrData data = new QrData.Builder()
                .label(email)
                .secret(secret)
                .issuer("TresorApp")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            QrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(data);
            return getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (Exception e) {
            log.error("TwoFactorService: error generating QR code", e);
            throw new RuntimeException("Could not generate QR code", e);
        }
    }

    public boolean verifyCode(String secret, String code) {
        CodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(), new SystemTimeProvider()
        );
        return verifier.isValidCode(secret, code);
    }
}

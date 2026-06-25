package ch.bbw.pr.tresorbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Verifies Google reCAPTCHA v2 tokens with Google's API.
 */
@Slf4j
@Service
public class CaptchaService {

    @Value("${google.recaptcha.secret}")
    private String recaptchaSecret;

    private static final String VERIFY_URL =
            "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s";

    public boolean verifyCaptcha(String token) {
        if (token == null || token.isBlank()) {
            log.warn("CaptchaService.verifyCaptcha: token is empty");
            return false;
        }
        try {
            String url = String.format(VERIFY_URL, recaptchaSecret, token);
            RestTemplate restTemplate = new RestTemplate();
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            boolean success = response != null && Boolean.TRUE.equals(response.get("success"));
            log.info("CaptchaService.verifyCaptcha: success={}", success);
            return success;
        } catch (Exception e) {
            log.error("CaptchaService.verifyCaptcha: error verifying captcha", e);
            return false;
        }
    }
}

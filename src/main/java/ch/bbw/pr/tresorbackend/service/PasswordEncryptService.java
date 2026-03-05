package ch.bbw.pr.tresorbackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

@Service
public class PasswordEncryptService {

    @Value("${auth.secret.masterkey}")
    private String masterKey;

    @Value("${auth.password.pepper}")
    private String pepper;

    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public PasswordEncryptService(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    // --- USER-PASSWÖRTER (Hashing) ---
    public String hashPassword(String rawPassword) {
        System.out.println("DEBUG hashPassword pepper: '" + pepper + "'");
        System.out.println("DEBUG hashPassword input: '" + rawPassword + pepper + "'");
        return passwordEncoder.encode(rawPassword + pepper);
    }

    public boolean checkPassword(String rawPassword, String hashedDbPassword) {
        System.out.println("DEBUG checkPassword pepper: '" + pepper + "'");
        System.out.println("DEBUG checkPassword input: '" + rawPassword + pepper + "'");
        return passwordEncoder.matches(rawPassword + pepper, hashedDbPassword);
    }

    // --- SECRETS (AES Verschlüsselung) ---
    public String encrypt(String strToEncrypt, String userSpecificPart) {
        try {
            SecretKeySpec secretKey = deriveKey(userSpecificPart);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return null;
        }
    }
   // --- SECRETS (Entschlüsselung & Key-Derivation) ---
    public String decrypt(String strToDecrypt, String userSpecificPart) {
        try {
            SecretKeySpec secretKey = deriveKey(userSpecificPart);
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
        } catch (Exception e) {
            return null;
        }
    }

    // --- Erzeugt einen eindeutigen AES-Key aus Masterkey + User-ID ---
    private SecretKeySpec deriveKey(String userPart) throws Exception {
        String combined = masterKey + userPart;
        byte[] key = combined.getBytes(StandardCharsets.UTF_8);
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16);
        return new SecretKeySpec(key, "AES");
    }
}
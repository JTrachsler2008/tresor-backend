package ch.bbw.pr.tresorbackend.util;

import org.jasypt.util.text.AES256TextEncryptor;

/**
 * EncryptUtil
 * Used to encrypt and decrypt secret content per user.
 * Uses AES-256 with the user's encryptPassword as key.
 * @author Peter Rutschmann
 */
public class EncryptUtil {

   private final AES256TextEncryptor encryptor;

   public EncryptUtil(String secretKey) {
      this.encryptor = new AES256TextEncryptor();
      this.encryptor.setPassword(secretKey);
   }

   public String encrypt(String data) {
      return encryptor.encrypt(data);
   }

   public String decrypt(String data) {
      return encryptor.decrypt(data);
   }
} 

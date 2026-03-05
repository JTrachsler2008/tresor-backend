package ch.bbw.pr.tresorbackend.controller;

import ch.bbw.pr.tresorbackend.model.Secret;
import ch.bbw.pr.tresorbackend.model.NewSecret;
import ch.bbw.pr.tresorbackend.model.EncryptCredentials;
import ch.bbw.pr.tresorbackend.model.User;
import ch.bbw.pr.tresorbackend.service.SecretService;
import ch.bbw.pr.tresorbackend.service.UserService;
import ch.bbw.pr.tresorbackend.util.EncryptUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SecretController
 * @author Peter Rutschmann
 */
@RestController
@AllArgsConstructor
@RequestMapping("api/secrets")
public class SecretController {

   private SecretService secretService;
   private UserService userService;

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping
   public ResponseEntity<String> createSecret(@Valid @RequestBody NewSecret newSecret, BindingResult bindingResult) {

      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
                 .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                 .collect(Collectors.toList());
         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         return ResponseEntity.badRequest().body(new Gson().toJson(obj));
      }

      User user = userService.findByEmail(newSecret.getEmail());
      if (user == null) return ResponseEntity.notFound().build();

      Secret secret = new Secret(
              null,
              user.getId(),
              new EncryptUtil(newSecret.getEncryptPassword()).encrypt(newSecret.getContent().toString())
      );

      secretService.createSecret(secret);
      JsonObject obj = new JsonObject();
      obj.addProperty("answer", "Secret saved");
      return ResponseEntity.accepted().body(new Gson().toJson(obj));
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/byuserid")
   public ResponseEntity<List<Secret>> getSecretsByUserId(@RequestBody EncryptCredentials credentials) {
      List<Secret> secrets = secretService.getSecretsByUserId(credentials.getUserId());
      if (secrets.isEmpty()) return ResponseEntity.notFound().build();

      for (Secret secret : secrets) {
         try {
            secret.setContent(new EncryptUtil(credentials.getEncryptPassword()).decrypt(secret.getContent()));
         } catch (EncryptionOperationNotPossibleException e) {
            secret.setContent("not encryptable. Wrong password?");
         }
      }
      return ResponseEntity.ok(secrets);
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/byemail")
   public ResponseEntity<List<Secret>> getSecretsByEmail(@RequestBody EncryptCredentials credentials) {
      User user = userService.findByEmail(credentials.getEmail());
      if (user == null) return ResponseEntity.notFound().build();

      List<Secret> secrets = secretService.getSecretsByUserId(user.getId());
      if (secrets.isEmpty()) return ResponseEntity.notFound().build();

      for (Secret secret : secrets) {
         try {
            secret.setContent(new EncryptUtil(credentials.getEncryptPassword()).decrypt(secret.getContent()));
         } catch (EncryptionOperationNotPossibleException e) {
            secret.setContent("not encryptable. Wrong password?");
         }
      }
      return ResponseEntity.ok(secrets);
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @GetMapping
   public ResponseEntity<List<Secret>> getAllSecrets() {
      List<Secret> secrets = secretService.getAllSecrets();
      return new ResponseEntity<>(secrets, HttpStatus.OK);
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PutMapping("{id}")
   public ResponseEntity<String> updateSecret(
           @PathVariable("id") Long secretId,
           @Valid @RequestBody NewSecret newSecret,
           BindingResult bindingResult) {

      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
                 .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                 .collect(Collectors.toList());
         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         return ResponseEntity.badRequest().body(new Gson().toJson(obj));
      }

      Secret dbSecret = secretService.getSecretById(secretId);
      if (dbSecret == null) {
         JsonObject obj = new JsonObject();
         obj.addProperty("answer", "Secret not found in db");
         return ResponseEntity.badRequest().body(new Gson().toJson(obj));
      }

      User user = userService.findByEmail(newSecret.getEmail());
      if (user == null) return ResponseEntity.notFound().build();

      if (!dbSecret.getUserId().equals(user.getId())) {
         JsonObject obj = new JsonObject();
         obj.addProperty("answer", "Secret has not same user id");
         return ResponseEntity.badRequest().body(new Gson().toJson(obj));
      }

      try {
         new EncryptUtil(newSecret.getEncryptPassword()).decrypt(dbSecret.getContent());
      } catch (EncryptionOperationNotPossibleException e) {
         JsonObject obj = new JsonObject();
         obj.addProperty("answer", "Password not correct.");
         return ResponseEntity.badRequest().body(new Gson().toJson(obj));
      }

      Secret secret = new Secret(
              secretId,
              user.getId(),
              new EncryptUtil(newSecret.getEncryptPassword()).encrypt(newSecret.getContent().toString())
      );
      secretService.updateSecret(secret);

      JsonObject obj = new JsonObject();
      obj.addProperty("answer", "Secret updated");
      return ResponseEntity.accepted().body(new Gson().toJson(obj));
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @DeleteMapping("{id}")
   public ResponseEntity<String> deleteSecret(@PathVariable("id") Long secretId) {
      secretService.deleteSecret(secretId);
      return new ResponseEntity<>("Secret successfully deleted!", HttpStatus.OK);
   }
}
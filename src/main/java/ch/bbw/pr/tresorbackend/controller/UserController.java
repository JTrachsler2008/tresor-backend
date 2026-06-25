package ch.bbw.pr.tresorbackend.controller;

import ch.bbw.pr.tresorbackend.model.*;
import ch.bbw.pr.tresorbackend.service.CaptchaService;
import ch.bbw.pr.tresorbackend.service.JwtService;
import ch.bbw.pr.tresorbackend.service.PasswordEncryptService;
import ch.bbw.pr.tresorbackend.service.PasswordResetService;
import ch.bbw.pr.tresorbackend.service.TwoFactorService;
import ch.bbw.pr.tresorbackend.service.UserService;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * UserController
 * @author Peter Rutschmann
 */
@RestController
@AllArgsConstructor
@RequestMapping("api/users")
public class UserController {

   private UserService userService;
   private PasswordEncryptService passwordService;
   private CaptchaService captchaService;
   private PasswordResetService passwordResetService;
   private JwtService jwtService;
   private TwoFactorService twoFactorService;
   private static final Logger logger = LoggerFactory.getLogger(UserController.class);


   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping
   public ResponseEntity<String> createUser(@Valid @RequestBody RegisterUser registerUser, BindingResult bindingResult) {

      // Verify reCAPTCHA token
      if (!captchaService.verifyCaptcha(registerUser.getRecaptchaToken())) {
         JsonObject obj = new JsonObject();
         obj.addProperty("message", "Captcha verification failed. Please try again.");
         logger.warn("UserController.createUser: captcha verification failed");
         return ResponseEntity.badRequest().body(new Gson().toJson(obj));
      }
      logger.info("UserController.createUser: captcha passed");

      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
                 .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                 .collect(Collectors.toList());
         System.out.println("UserController.createUser " + errors);

         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         String json = new Gson().toJson(obj);

         System.out.println("UserController.createUser, validation fails: " + json);
         return ResponseEntity.badRequest().body(json);
      }
      System.out.println("UserController.createUser: input validation passed");


      System.out.println("UserController.createUser, password validation passed");


      User user = new User(
              null,
              registerUser.getFirstName(),
              registerUser.getLastName(),
              registerUser.getEmail(),
              passwordService.hashPassword(registerUser.getPassword()),
              "ROLE_USER",
              false,
              null
      );

      User savedUser = userService.createUser(user);
      JsonObject obj = new JsonObject();
      if (savedUser != null) {
         System.out.println("UserController.createUser, user saved in db");
         obj.addProperty("answer", "User saved");
      } else {
         System.out.println("UserController.createUser, user not saved in db");
         obj.addProperty("answer", "User not saved");
      }
      String json = new Gson().toJson(obj);
      System.out.println("UserController.createUser " + json);
      return ResponseEntity.accepted().body(json);
   }


   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @GetMapping("{id}")
   public ResponseEntity<User> getUserById(@PathVariable("id") Long userId) {
      User user = userService.getUserById(userId);
      if (user == null) return ResponseEntity.notFound().build();
      return new ResponseEntity<>(user, HttpStatus.OK);
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @GetMapping
   public ResponseEntity<List<User>> getAllUsers() {
      List<User> users = userService.getAllUsers();
      if (users.isEmpty()) return ResponseEntity.notFound().build();
      return new ResponseEntity<>(users, HttpStatus.OK);
   }


   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PutMapping("{id}")
   public ResponseEntity<User> updateUser(@PathVariable("id") Long userId,
                                          @RequestBody User user) {
      user.setId(userId);
      User updatedUser = userService.updateUser(user);
      if (updatedUser == null) return ResponseEntity.notFound().build();
      return new ResponseEntity<>(updatedUser, HttpStatus.OK);
   }


   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @DeleteMapping("{id}")
   public ResponseEntity<String> deleteUser(@PathVariable("id") Long userId) {
      if( userService.deleteUser(userId))
         return new ResponseEntity<>("User successfully deleted!", HttpStatus.OK);
      return ResponseEntity.notFound().build();
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/byemail")
   public ResponseEntity<String> getUserIdByEmail(@RequestBody EmailAdress email, BindingResult bindingResult) {
      System.out.println("UserController.getUserIdByEmail: " + email);

      if (bindingResult.hasErrors()) {
         List<String> errors = bindingResult.getFieldErrors().stream()
                 .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                 .collect(Collectors.toList());
         System.out.println("UserController.createUser " + errors);

         JsonArray arr = new JsonArray();
         errors.forEach(arr::add);
         JsonObject obj = new JsonObject();
         obj.add("message", arr);
         String json = new Gson().toJson(obj);

         System.out.println("UserController.createUser, validation fails: " + json);
         return ResponseEntity.badRequest().body(json);
      }

      System.out.println("UserController.getUserIdByEmail: input validation passed");

      User user = userService.findByEmail(email.getEmail());
      if (user == null) {
         System.out.println("UserController.getUserIdByEmail, no user found with email: " + email);
         JsonObject obj = new JsonObject();
         obj.addProperty("message", "No user found with this email");
         String json = new Gson().toJson(obj);

         System.out.println("UserController.getUserIdByEmail, fails: " + json);
         return ResponseEntity.badRequest().body(json);
      }
      System.out.println("UserController.getUserIdByEmail, user find by email");
      JsonObject obj = new JsonObject();
      obj.addProperty("answer", user.getId());
      String json = new Gson().toJson(obj);
      System.out.println("UserController.getUserIdByEmail " + json);
      return ResponseEntity.accepted().body(json);
   }


   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/forgot-password")
   public ResponseEntity<String> forgotPassword(@RequestBody Map<String, String> body) {
      String email = body.get("email");
      if (email == null || email.isBlank()) {
         return ResponseEntity.badRequest().body("{\"message\":\"Email is required.\"}");
      }
      PasswordResetService.RequestResult result = passwordResetService.requestPasswordReset(email);
      JsonObject obj = new JsonObject();
      switch (result) {
         case SENT -> obj.addProperty("message", "Reset email sent.");
         case RATE_LIMITED -> obj.addProperty("message", "Please wait a few minutes before requesting another reset email.");
         case USER_NOT_FOUND -> obj.addProperty("message", "Reset email sent.");  // don't leak user existence
      }
      return ResponseEntity.ok(new Gson().toJson(obj));
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/reset-password")
   public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> body) {
      String token = body.get("token");
      String newPassword = body.get("newPassword");
      if (token == null || newPassword == null) {
         return ResponseEntity.badRequest().body("{\"message\":\"Token and newPassword are required.\"}");
      }
      // Validate password strength
      if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-])[A-Za-z\\d@$!%*?&._\\-]{8,}$")) {
         JsonObject obj = new JsonObject();
         obj.addProperty("message", "Password must have at least 8 characters, one uppercase, one lowercase, one digit and one special character.");
         return ResponseEntity.badRequest().body(new Gson().toJson(obj));
      }
      PasswordResetService.ResetResult result = passwordResetService.resetPassword(token, newPassword);
      JsonObject obj = new JsonObject();
      return switch (result) {
         case SUCCESS -> { obj.addProperty("message", "Password reset successfully."); yield ResponseEntity.ok(new Gson().toJson(obj)); }
         case TOKEN_NOT_FOUND -> { obj.addProperty("message", "Invalid or expired reset link."); yield ResponseEntity.badRequest().body(new Gson().toJson(obj)); }
         case TOKEN_EXPIRED -> { obj.addProperty("message", "Reset link has expired. Please request a new one."); yield ResponseEntity.badRequest().body(new Gson().toJson(obj)); }
      };
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/login")
   public ResponseEntity<LoginResponse> doLoginUser(@Valid @RequestBody LoginUser loginUser, BindingResult bindingResult) {
      System.out.println("UserController.doLoginUser: " + loginUser.getEmail());

      if (bindingResult.hasErrors()) {
         String errorMessage = bindingResult.getFieldErrors().stream()
                 .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                 .collect(Collectors.joining("; "));
         return ResponseEntity.badRequest().body(new LoginResponse(errorMessage, null, null, null, false, null));
      }

      User user = userService.findByEmail(loginUser.getEmail());
      if (user == null) {
         System.out.println("UserController.doLoginUser: user not found");
         return ResponseEntity.badRequest().body(new LoginResponse("Invalid email or password", null, null, null, false, null));
      }

      if (!passwordService.checkPassword(loginUser.getPassword(), user.getPassword())) {
         System.out.println("UserController.doLoginUser: wrong password");
         return ResponseEntity.badRequest().body(new LoginResponse("Invalid email or password", null, null, null, false, null));
      }

      // If 2FA is enabled, return a short-lived temp token instead of the full JWT
      if (user.isTwoFactorEnabled()) {
         String tempToken = jwtService.generateTempToken(user.getEmail());
         logger.info("UserController.doLoginUser: 2FA required for {}", user.getEmail());
         return ResponseEntity.ok(new LoginResponse("2FA required", null, null, null, true, tempToken));
      }

      String token = jwtService.generateToken(user.getEmail(), user.getRole());
      logger.info("UserController.doLoginUser: login successful, role={}", user.getRole());
      return ResponseEntity.ok(new LoginResponse("Login successful", user.getId(), token, user.getRole(), false, null));
   }

   // ── 2FA Endpoints ──────────────────────────────────────────────────────────

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/2fa/setup")
   public ResponseEntity<String> setup2fa(@RequestBody Map<String, String> body) {
      String email = body.get("email");
      User user = userService.findByEmail(email);
      if (user == null) return ResponseEntity.badRequest().body("{\"message\":\"User not found\"}");

      String secret = twoFactorService.generateSecret();
      user.setTwoFactorSecret(secret);
      userService.updateUser(user);

      String qrCode = twoFactorService.generateQrCodeDataUri(email, secret);
      JsonObject obj = new JsonObject();
      obj.addProperty("qrCode", qrCode);
      obj.addProperty("secret", secret);
      return ResponseEntity.ok(new Gson().toJson(obj));
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/2fa/enable")
   public ResponseEntity<String> enable2fa(@RequestBody Map<String, String> body) {
      String email = body.get("email");
      String code  = body.get("code");
      User user = userService.findByEmail(email);
      if (user == null || user.getTwoFactorSecret() == null)
         return ResponseEntity.badRequest().body("{\"message\":\"Setup not started\"}");

      if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), code))
         return ResponseEntity.badRequest().body("{\"message\":\"Invalid code\"}");

      user.setTwoFactorEnabled(true);
      userService.updateUser(user);
      return ResponseEntity.ok("{\"message\":\"2FA enabled\"}");
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/2fa/verify")
   public ResponseEntity<LoginResponse> verify2fa(@RequestBody Map<String, String> body) {
      String tempToken = body.get("tempToken");
      String code      = body.get("code");

      if (!jwtService.isTokenValid(tempToken))
         return ResponseEntity.badRequest().body(new LoginResponse("Invalid or expired session", null, null, null, false, null));

      String email = jwtService.extractEmail(tempToken);
      User user = userService.findByEmail(email);
      if (user == null)
         return ResponseEntity.badRequest().body(new LoginResponse("User not found", null, null, null, false, null));

      if (!twoFactorService.verifyCode(user.getTwoFactorSecret(), code))
         return ResponseEntity.badRequest().body(new LoginResponse("Invalid 2FA code", null, null, null, true, tempToken));

      String token = jwtService.generateToken(user.getEmail(), user.getRole());
      return ResponseEntity.ok(new LoginResponse("Login successful", user.getId(), token, user.getRole(), false, null));
   }

   @CrossOrigin(origins = "${CROSS_ORIGIN}")
   @PostMapping("/2fa/disable")
   public ResponseEntity<String> disable2fa(@RequestBody Map<String, String> body) {
      String email = body.get("email");
      User user = userService.findByEmail(email);
      if (user == null) return ResponseEntity.badRequest().body("{\"message\":\"User not found\"}");
      user.setTwoFactorEnabled(false);
      user.setTwoFactorSecret(null);
      userService.updateUser(user);
      return ResponseEntity.ok("{\"message\":\"2FA disabled\"}");
   }

}
package ch.bbw.pr.tresorbackend.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * RegisterUser
 * @author Peter Rutschmann
 */
@Value
public class RegisterUser {


   @NotEmpty(message="Firstname is required.")
   @Size(min=2, max=25, message="Firstname size has to be 2 up to 25 characters.")
   private String firstName;

   @NotEmpty (message="Lastname is required.")
   @Size(min=2, max=25, message="Lastname size has to be 2 up to 25 characters.")
   private String lastName;

   @NotEmpty (message="E-Mail is required.")
   private String email;

   @NotEmpty (message="Password is required.")
   @Pattern(
      regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&._\\-])[A-Za-z\\d@$!%*?&._\\-]{8,}$",
      message = "Password must have at least 8 characters, one uppercase letter, one lowercase letter, one digit and one special character (@$!%*?&._-)."
   )
   private String password;

   @NotEmpty (message="Password-confirmation is required.")
   private String passwordConfirmation;

   private String recaptchaToken;
}
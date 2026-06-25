package ch.bbw.pr.tresorbackend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String message;
    private Long userId;
    private String token;
    private String role;
    private boolean requiresTwoFactor;
    private String tempToken;
}

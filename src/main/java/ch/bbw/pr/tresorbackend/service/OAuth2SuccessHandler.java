package ch.bbw.pr.tresorbackend.service;

import ch.bbw.pr.tresorbackend.model.User;
import ch.bbw.pr.tresorbackend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * After Google OAuth2 login: find or create user, generate JWT, redirect to frontend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${auth.password.pepper}")
    private String pepper;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email     = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName  = oAuth2User.getAttribute("family_name");
        if (lastName == null) lastName = "";

        Optional<User> existing = userRepository.findByEmail(email);
        User user;
        if (existing.isPresent()) {
            user = existing.get();
        } else {
            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString() + pepper);
            user = new User(null, firstName, lastName, email, randomPassword, "ROLE_USER", false, null);
            userRepository.save(user);
            log.info("OAuth2SuccessHandler: new user created via Google: {}", email);
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        String redirectUrl = baseUrl + "/oauth2/callback?token=" + token
                + "&userId=" + user.getId()
                + "&role=" + user.getRole();
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

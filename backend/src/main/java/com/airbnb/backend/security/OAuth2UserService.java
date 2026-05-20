package com.airbnb.backend.security;

import com.airbnb.backend.entity.Role;
import com.airbnb.backend.entity.User;
import com.airbnb.backend.enums.RoleName;
import com.airbnb.backend.repository.RoleRepository;
import com.airbnb.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest)
            throws OAuth2AuthenticationException {


        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();


        String providerId = (String) attributes.get("sub"); // Google's unique user ID
        String email     = (String) attributes.get("email");
        String firstName = (String) attributes.get("given_name");
        String lastName  = (String) attributes.get("family_name");

        if (lastName == null) lastName = "";

        log.info("OAuth2 login attempt for email: {}", email);


        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = registerOAuth2User(email, firstName, lastName, providerId);
            log.info("New OAuth2 user registered: {}", email);
        } else {
            user.setProviderId(providerId);
            user.setProvider("GOOGLE");
            if (user.getFirstName() == null) user.setFirstName(firstName);
            if (user.getLastName() == null) user.setLastName(lastName);
            userRepository.save(user);
            log.info("Existing OAuth2 user logged in: {}", email);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerOAuth2User(String email, String firstName,
                                    String lastName, String providerId) {
        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new RuntimeException(
                        "Default role not found. Make sure DataInitializer ran."));

        User newUser = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .provider("GOOGLE")
                .providerId(providerId)
                .isActive(true)
                .isEmailVerified(true) // Google already verified the email
                .roles(Set.of(userRole))
                .build();

        return userRepository.save(newUser);
    }
}
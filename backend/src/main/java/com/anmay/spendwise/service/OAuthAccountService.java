package com.anmay.spendwise.service;

import com.anmay.spendwise.entity.AppUser;
import com.anmay.spendwise.repository.AppUserRepository;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class OAuthAccountService {
    private final AppUserRepository userRepository;
    private final AccountProvisioningService provisioningService;

    public OAuthAccountService(AppUserRepository userRepository,
                               AccountProvisioningService provisioningService) {
        this.userRepository = userRepository;
        this.provisioningService = provisioningService;
    }

    @Transactional
    public AppUser upsertGoogleUser(OAuth2User oauth2User) {
        String subject = requiredAttribute(oauth2User, "sub");
        String email = requiredAttribute(oauth2User, "email").toLowerCase(Locale.ROOT);
        String name = oauth2User.getAttribute("name");
        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf('@'));
        }

        AppUser user = userRepository.findByGoogleSubject(subject)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(email).orElse(null));

        if (user == null) {
            user = userRepository.save(AppUser.googleUser(name.trim(), email, subject));
            provisioningService.provision(user);
            return user;
        }

        if (user.getGoogleSubject() == null) {
            user.setGoogleSubject(subject);
        }
        user.setName(name.trim());
        return userRepository.save(user);
    }

    private String requiredAttribute(OAuth2User user, String attribute) {
        String value = user.getAttribute(attribute);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Google did not provide " + attribute);
        }
        return value;
    }
}

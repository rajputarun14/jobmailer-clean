package com.arun.jobmailer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.arun.jobmailer.model.UserAccount;
import com.arun.jobmailer.repository.UserRepository;

class UserServiceTest {

    @Test
    void storesLoginPasswordHashedAndGmailPasswordEncrypted() {
        UserRepository repo = org.mockito.Mockito.mock(UserRepository.class);
        final UserAccount[] saved = new UserAccount[1];
        when(repo.save(any(UserAccount.class))).thenAnswer(invocation -> {
            saved[0] = invocation.getArgument(0);
            return saved[0];
        });
        when(repo.findById("arun@gmail.com")).thenAnswer(invocation -> java.util.Optional.ofNullable(saved[0]));
        CredentialEncryptionService encryptionService = new CredentialEncryptionService("test-secret-key");
        UserService service = new UserService(repo, new BCryptPasswordEncoder(), encryptionService);

        service.createUser("ARUN@GMAIL.COM", "login-password", "gmail-app-password", "USER");

        var user = saved[0];
        assertThat(user.getPassword()).isNotEqualTo("login-password");
        assertThat(user.getEncryptedGmailAppPassword()).isNotEqualTo("gmail-app-password");
        assertThat(service.getGmailAppPassword("arun@gmail.com")).isEqualTo("gmail-app-password");
    }
}

package com.arun.jobmailer.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CredentialEncryptionServiceTest {

    @Test
    void encryptsAndDecryptsCredential() {
        CredentialEncryptionService service = new CredentialEncryptionService("test-secret-key");

        String encrypted = service.encrypt("gmail-app-password");

        assertThat(encrypted).isNotEqualTo("gmail-app-password");
        assertThat(service.decrypt(encrypted)).isEqualTo("gmail-app-password");
    }
}

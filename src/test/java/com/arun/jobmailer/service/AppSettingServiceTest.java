package com.arun.jobmailer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.arun.jobmailer.model.AppSetting;
import com.arun.jobmailer.repository.AppSettingRepository;

class AppSettingServiceTest {

    @Test
    void storesGeminiApiKeyEncryptedAndReturnsDecryptedValue() {
        AppSettingRepository repository = org.mockito.Mockito.mock(AppSettingRepository.class);
        Map<String, AppSetting> store = new HashMap<>();
        when(repository.findById(AppSettingService.GEMINI_API_KEY))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(AppSettingService.GEMINI_API_KEY)));
        when(repository.existsById(AppSettingService.GEMINI_API_KEY))
                .thenAnswer(invocation -> store.containsKey(AppSettingService.GEMINI_API_KEY));
        when(repository.save(any(AppSetting.class))).thenAnswer(invocation -> {
            AppSetting setting = invocation.getArgument(0);
            store.put(setting.getSettingKey(), setting);
            return setting;
        });

        AppSettingService service = new AppSettingService(repository, new CredentialEncryptionService("test-secret"));

        service.saveGeminiApiKey("gemini-key");

        AppSetting stored = store.get(AppSettingService.GEMINI_API_KEY);
        assertThat(stored.getEncryptedValue()).isNotEqualTo("gemini-key");
        assertThat(service.hasGeminiApiKey()).isTrue();
        assertThat(service.getGeminiApiKey()).contains("gemini-key");
    }
}

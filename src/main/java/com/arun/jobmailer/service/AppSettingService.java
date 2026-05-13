package com.arun.jobmailer.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.arun.jobmailer.model.AppSetting;
import com.arun.jobmailer.repository.AppSettingRepository;

@Service
public class AppSettingService {

    public static final String GEMINI_API_KEY = "gemini.api-key";

    private final AppSettingRepository repository;
    private final CredentialEncryptionService credentialEncryptionService;

    public AppSettingService(AppSettingRepository repository, CredentialEncryptionService credentialEncryptionService) {
        this.repository = repository;
        this.credentialEncryptionService = credentialEncryptionService;
    }

    public void saveGeminiApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Gemini API key cannot be blank");
        }
        saveEncrypted(GEMINI_API_KEY, apiKey.trim());
    }

    public Optional<String> getGeminiApiKey() {
        return getDecrypted(GEMINI_API_KEY);
    }

    public boolean hasGeminiApiKey() {
        return repository.existsById(GEMINI_API_KEY);
    }

    private void saveEncrypted(String key, String value) {
        AppSetting setting = repository.findById(key)
                .orElseGet(() -> new AppSetting(key, ""));
        setting.setEncryptedValue(credentialEncryptionService.encrypt(value));
        setting.setUpdatedAt(Instant.now());
        repository.save(setting);
    }

    private Optional<String> getDecrypted(String key) {
        return repository.findById(key)
                .map(AppSetting::getEncryptedValue)
                .filter(value -> value != null && !value.isBlank())
                .map(credentialEncryptionService::decrypt);
    }
}

package com.arun.jobmailer.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_setting")
public class AppSetting {

    @Id
    @Column(length = 120)
    private String settingKey;

    @Column(length = 4096)
    private String encryptedValue;

    private Instant updatedAt;

    public AppSetting() {}

    public AppSetting(String settingKey, String encryptedValue) {
        this.settingKey = settingKey;
        this.encryptedValue = encryptedValue;
        this.updatedAt = Instant.now();
    }

    public String getSettingKey() { return settingKey; }
    public void setSettingKey(String settingKey) { this.settingKey = settingKey; }
    public String getEncryptedValue() { return encryptedValue; }
    public void setEncryptedValue(String encryptedValue) { this.encryptedValue = encryptedValue; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

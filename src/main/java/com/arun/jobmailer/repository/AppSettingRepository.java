package com.arun.jobmailer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.arun.jobmailer.model.AppSetting;

@Repository
public interface AppSettingRepository extends JpaRepository<AppSetting, String> {
}

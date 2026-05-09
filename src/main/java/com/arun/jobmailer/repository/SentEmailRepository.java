package com.arun.jobmailer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.arun.jobmailer.model.SentEmail;

@Repository
public interface SentEmailRepository extends JpaRepository<SentEmail, String> {
    List<SentEmail> findAllByOrderBySentAtDesc();
    List<SentEmail> findAllByOwnerOrderBySentAtDesc(String owner);
}

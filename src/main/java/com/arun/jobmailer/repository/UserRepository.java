package com.arun.jobmailer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.arun.jobmailer.model.UserAccount;

@Repository
public interface UserRepository extends JpaRepository<UserAccount, String> {
}

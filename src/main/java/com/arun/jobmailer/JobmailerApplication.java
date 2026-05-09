package com.arun.jobmailer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

import com.arun.jobmailer.service.UserService;

@SpringBootApplication
public class JobmailerApplication {

	private static final Logger log = LoggerFactory.getLogger(JobmailerApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(JobmailerApplication.class, args);
	}

	@Bean
	CommandLineRunner seedAdmin(UserService userService,
								@Value("${APP_USER:arun}") String username,
								@Value("${APP_PASSWORD:jobmailer123}") String password) {
		return args -> {
			if (userService.find(username) == null) {
				userService.createUser(username, password, "ADMIN");
				log.info("Seeded admin user: {}", username);
			}
		};
	}

	@Bean
	CommandLineRunner s3TestRunner(com.arun.jobmailer.service.S3Service s3Service,
				@Value("${aws.s3.test-upload:false}") boolean testUpload,
				@Value("${aws.s3.test-key:Arun__Kumar.pdf}") String testKey,
				@Value("${aws.s3.test-file:resume/Arun__Kumar.pdf}") String testFile) {
		return args -> {
			if (!testUpload) return;
			java.io.File f = new java.io.File(testFile);
			if (!f.exists()) {
				log.warn("S3 test file not found: {}", testFile);
				return;
			}
			try {
				s3Service.uploadFile(testKey, f);
				log.info("Uploaded test file to S3 with key: {}", testKey);
			} catch (Exception e) {
				log.error("S3 test upload failed", e);
			}
		};
	}

}

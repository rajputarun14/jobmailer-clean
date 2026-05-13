# JobMailer

JobMailer is a simple Spring Boot application that allows users to quickly send job application emails to recruiters using predefined templates.

## Features

- Send job application emails instantly
- Multiple email templates
- Different subject/body/resume per template
- Gmail SMTP integration
- Basic authentication security
- Simple UI for quick usage

## Tech Stack

- Java 17
- Spring Boot
- Spring Mail
- Spring Security
- Maven
- HTML + JavaScript

## How It Works

1. Select email template
2. Enter recruiter email
3. Click Send
4. Email is sent automatically with the selected template and resume attached

## Running Locally

Clone the repository

Start Postgres:

```bash
docker compose up -d
```

Run the app with Gmail SMTP credentials:

```bash
EMAIL_USERNAME="your_email@gmail.com" \
EMAIL_PASSWORD="your_16_character_gmail_app_password" \
./mvnw spring-boot:run
```

For Gmail, `EMAIL_PASSWORD` must be an App Password, not your normal Gmail password.

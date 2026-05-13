# JobMailer — AI-Powered Job Application Automation Platform

JobMailer is an AI-powered job application automation platform that simplifies and accelerates the entire job application workflow.

The idea originated from a real-world frustration while manually applying to jobs every day through LinkedIn and other job portals. Recruiters often post Job Descriptions along with their email addresses, requiring candidates to repeatedly draft emails, customize resumes, attach documents, and send applications manually.

JobMailer automates this entire process into a one-click workflow.

---

# Features

* AI-powered ATS-friendly resume optimization
* Automatic email subject and body generation based on Job Description
* One-click recruiter email sending
* Resume tailoring according to the provided JD
* PDF resume generation
* Dashboard to track sent applications
* One-click recruiter follow-up emails
* Multiple email templates
* Gmail SMTP integration
* Basic authentication & security
* Clean and simple UI

---

# How It Works

1. Upload your resume
2. Paste the Job Description
3. Enter recruiter email
4. Click Send

JobMailer automatically:

* analyzes the Job Description,
* generates a personalized email,
* optimizes the resume using AI,
* creates an ATS-friendly PDF resume,
* attaches it to the email,
* and sends the application automatically.

The dashboard also allows users to:

* view all sent applications,
* track recruiter emails,
* monitor timestamps,
* and send follow-up emails with a single click.

---

# Tech Stack

## Backend

* Java 17
* Spring Boot
* Spring Security
* Spring Mail
* Maven

## Frontend

* HTML
* CSS
* JavaScript

## AI Integration

* Gemini API / OpenAI API

## PDF & Resume Processing

* Apache POI
* PDFBox
* OpenHTMLToPDF

## Database

* MySQL

---

# Architecture Overview

```text
Frontend UI
     ↓
Spring Boot Backend
     ↓
Resume Parser Service
     ↓
AI Resume Tailoring Engine
     ↓
ATS Resume PDF Generator
     ↓
Email Service (SMTP)
     ↓
Dashboard & Application Tracking
```

---

# Key Challenges Solved

* Automating repetitive job application workflows
* AI-based resume optimization while preventing hallucinated experience
* ATS-friendly resume formatting
* Dynamic email generation based on Job Description
* One-click follow-up workflow

---

# Running Locally

## Clone Repository

```bash
git clone https://github.com/yourusername/jobmailer.git
```

## Configure Application Properties

Update:

* SMTP credentials
* Database configuration
* AI API keys

## Run Backend

```bash
mvn spring-boot:run
```

## Open Application

```text
http://localhost:8080
```

---

# Future Enhancements

* Resume match scoring
* LinkedIn integration
* AI-generated cover letters
* Auto follow-up scheduling
* Multiple resume templates
* Analytics dashboard
* Chrome extension for LinkedIn job posts

---

# Why This Project Matters

JobMailer was built to solve a practical, real-world problem using automation and AI. The project combines backend engineering, AI integration, resume processing, PDF generation, email systems, and workflow automation into a single product-focused application.

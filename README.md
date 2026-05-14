# MindForge — JavaFX Desktop Application

> A full-featured productivity and learning platform built with JavaFX 17, Maven, and MySQL/MariaDB.

---

## Table of Contents

- [Overview](#overview)
- [Modules](#modules)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Database Setup](#database-setup)
- [Environment Variables](#environment-variables)
- [Running the Application](#running-the-application)
- [Project Structure](#project-structure)
- [Default Accounts](#default-accounts)
- [Branch Strategy](#branch-strategy)

---

## Overview

MindForge is a desktop learning platform that combines task planning, focus sessions, virtual study rooms, a resource library, career management, and community features — all in a single JavaFX application.

---

## Modules

| Module | Description |
|---|---|
| **Architect** | User authentication, profiles, avatar builder, emotion/mental health check |
| **Planner** | Task management, exam scheduling, calendar view, AI study assistant |
| **Guardian** | Focus timer (Pomodoro), virtual study rooms, resource library, AI insights |
| **Community** | Shared challenges, support tickets, virtual room chat |
| **Careers** | Job opportunities, company profiles, applications, career quiz |
| **Analyst** | Gamification — XP, levels, badges, leaderboard |
| **Admin** | User management, role requests, AI-powered platform analytics |

---

## Tech Stack

- **Language:** Java 17
- **UI:** JavaFX 17.0.6 + FXML
- **Build:** Maven 3.9+
- **Database:** MySQL / MariaDB (via XAMPP)
- **ORM:** Hibernate 5.6 (Community module)
- **AI:** Groq API (Llama 3), OpenAI-compatible
- **Computer Vision:** OpenCV 4.11.0 (emotion detection)
- **Auth:** BCrypt, Google OAuth 2.0
- **Real-time:** Pusher, Daily.co, Twilio, Agora

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| Java JDK | 17+ | Oracle or OpenJDK |
| Maven | 3.9+ | `mvn -version` to check |
| XAMPP | Any | MySQL/MariaDB must be running on port 3306 |
| OpenCV | 4.11.0 | Install to `C:\opencv\` — required for camera/emotion features |

---

## Getting Started

```bash
# 1. Clone the repository
git clone https://github.com/chedi-khlifi/Pii-java.git
cd Pii-java

# 2. Switch to the dev branch
git checkout dev

# 3. Install dependencies
mvn dependency:resolve
```

---

## Database Setup

### 1. Start XAMPP MySQL

Make sure MySQL is running on `localhost:3306`.

### 2. Create the database

```sql
CREATE DATABASE IF NOT EXISTS mindforge_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Seed the database

```bash
# Windows CMD (not PowerShell)
C:\xampp\mysql\bin\mysql.exe -u root mindforge_db < populate.sql
```

The `populate.sql` file at the project root seeds:
- 5 users (admin + 4 students)
- 8 subjects, 10 tasks, 5 exams
- 5 virtual rooms with participants
- 6 resources, 7 focus sessions
- 5 badges, gamification stats, shared challenges

### 4. Database connection

The app connects using:
```
Host:     localhost
Port:     3306
Database: mindforge_db
User:     root
Password: (empty)
```

To change these, edit `src/main/resources/config.properties` and `src/main/resources/META-INF/persistence.xml`.

---

## Environment Variables

Copy `GUARDIAN_ENV.example` to a `.env` file or set these as system environment variables / IntelliJ run configuration VM options.

### AI

| Variable | Description |
|---|---|
| `OPENAI_API_KEY` | OpenAI or Groq API key for AI features |
| `GROQ_API_KEY` | Groq API key (used by Focus Timer AI and Emotion module) |

### Google Services

| Variable | Description |
|---|---|
| `GOOGLE_OAUTH_TOKEN` | OAuth access token — used by Google Drive and Google Calendar clients |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID (for Google Sign-In) |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret |

### Microsoft

| Variable | Description |
|---|---|
| `MICROSOFT_GRAPH_TOKEN` | Microsoft Graph access token for Outlook calendar integration |

### Notifications

| Variable | Description |
|---|---|
| `FIREBASE_SERVER_KEY` | Firebase Cloud Messaging key for push notifications |
| `SENDGRID_API_KEY` | SendGrid key for email delivery |

### Storage

| Variable | Description |
|---|---|
| `DROPBOX_OAUTH_TOKEN` | Dropbox OAuth token for resource sync |

### Video / Real-time

| Variable | Description |
|---|---|
| `AGORA_APP_ID` | Agora App ID for voice rooms |
| `AGORA_APP_CERT` | Agora App Certificate |
| `TWILIO_ACCOUNT_SID` | Twilio Account SID for video calls |
| `TWILIO_AUTH_TOKEN` | Twilio Auth Token |
| `DAILYCO_API_KEY` | Daily.co API key for video sessions |

### Security

| Variable | Description |
|---|---|
| `GUARDIAN_JWT_SECRET` | JWT secret for Guardian module tokens. Has a dev fallback — set your own for production. |

> **Note:** `GOOGLE_OAUTH_TOKEN` is shared between Google Drive and Google Calendar clients.

---

## Running the Application

### With Maven (recommended)

```bash
mvn javafx:run
```

### With OpenCV camera support

OpenCV 4.11.0 must be installed at `C:\opencv\`. The `pom.xml` already includes the VM option:

```
-Djava.library.path=C:\opencv\build\java\x64
```

If the camera still doesn't load, verify the DLL exists:

```
C:\opencv\build\java\x64\opencv_java4110.dll
```

---

## Project Structure

```
src/main/java/
├── com/mindforge/
│   ├── architect/controller/   # Login, Register, Profile, Emotions, Dashboard
│   ├── controllers/carriere/   # Careers module controllers
│   ├── service/                # Email, Google OAuth
│   └── utils/                  # DB connection, session manager
├── com/example/                # Community module (Hibernate-based)
│   ├── controllers/            # Chat, Claims, Rooms, SharedTask
│   ├── entity/                 # JPA entities
│   └── service/                # Business logic
├── example/                    # Planner module
│   ├── PlannerModule.java      # Full Planner UI (session-aware)
│   ├── TaskController.java     # Task CRUD (owner-filtered)
│   └── ExamController.java     # Exam CRUD (owner-filtered)
└── tn/esprit/
    ├── GuardianController.java # Focus Timer, Rooms, Library
    └── services/guardian/      # FocusSession, VirtualRoom, Resource, AI services

src/main/resources/
├── com/mindforge/
│   ├── fxml/                   # All main app FXML pages
│   └── css/style.css           # Global stylesheet
├── tn/esprit/view/             # Guardian module FXML pages
├── views/                      # Community module FXML pages
└── config.properties           # App configuration
```

---

## Default Accounts

After running `populate.sql`, these accounts are available:

| Email | Password | Role |
|---|---|---|
| `admin@mindforge.local` | `password123` | Admin |
| `alice@mindforge.local` | `password123` | Student |
| `bob@mindforge.local` | `password123` | Student |
| `carol@mindforge.local` | `password123` | Student |
| `student2@mindforge.local` | `password123` | Student |

---

## Branch Strategy

| Branch | Purpose |
|---|---|
| `main` / `rjab` | Stable base |
| `dev` | Active development — all latest features |

Always pull from `dev` for the latest work:

```bash
git pull origin dev
```

---

## License

Proprietary — ESPRIT School of Engineering, 2025–2026.

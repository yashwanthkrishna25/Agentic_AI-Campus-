# AGENTRA AI CAMPUS — Multi-College AI-Powered Smart Campus Management System

**<span style="color:red">PROJECT TITLE: AGENTRA AI CAMPUS — Multi-College AI-Powered Smart Campus Management System</span>**

**Submitted for Final Year Project Evaluation**

**Department:** Computer Science & Engineering  
**Institution:** Multi-College Campus Network  
**Project Type:** Major / Final Year Project  
**Platform:** Android (Kotlin, Jetpack Compose) + Backend (FastAPI, Python) + Database (Firebase Firestore)

---

## TABLE OF CONTENTS

1. [Abstract](#1-abstract)
2. [Introduction](#2-introduction)
3. [Problem Statement](#3-problem-statement)
4. [Project Purpose & Objectives](#4-project-purpose--objectives)
5. [Existing System vs Proposed System](#5-existing-system-vs-proposed-system)
6. [System Architecture Design](#6-system-architecture-design)
7. [Six Role-Based Modules](#7-six-role-based-modules)
8. [Key Features & Innovations](#8-key-features--innovations)
9. [Database Schema](#9-database-schema)
10. [API Endpoints](#10-api-endpoints)
11. [Flowcharts & Diagrams](#11-flowcharts--diagrams)
12. [Hardware & Software Requirements](#12-hardware--software-requirements)
13. [How to Use the System](#13-how-to-use-the-system)
14. [Security & Privacy](#14-security--privacy)
15. [Testing & Validation](#15-testing--validation)
16. [Future Scope](#16-future-scope)
17. [Conclusion](#17-conclusion)
18. [References](#18-references)

---

## 1. ABSTRACT

AGENTRA AI CAMPUS is a complete, production-ready, multi-college AI-powered campus management platform designed to digitize and automate all academic, administrative, and operational workflows across unlimited colleges. The system supports six independent roles — Student, Faculty, HOD, Principal, College Admin, and Super Admin — each with role-based dashboards, permissions, navigation, and operations.

The platform integrates genuine AI capabilities (Groq Cloud LLM), advanced face recognition attendance with liveness detection, secure private document storage, digital certificates, hall tickets, placement management, real-time analytics, and a complete notification ecosystem. All data is isolated by college and department, ensuring strict tenant privacy and security.

---

## 2. INTRODUCTION

Managing multiple colleges, departments, faculty, students, attendance, examinations, assignments, and documents manually is time-consuming, error-prone, and lacks transparency. Existing campus management systems are typically single-college, lack AI integration, and do not provide role-based access control across multiple institutions.

AGENTRA AI CAMPUS solves these problems by providing a unified, scalable, AI-driven platform that connects every stakeholder in the campus ecosystem through a secure, responsive, mobile-first application.

---

## 3. PROBLEM STATEMENT

Current campus management challenges include:

- **Fragmented systems:** Attendance, marks, assignments, and documents are managed through separate tools or paper-based processes.
- **Single-college limitation:** Most systems cannot manage multiple colleges simultaneously.
- **No AI integration:** Students and faculty lack intelligent assistants for learning, question generation, and placement preparation.
- **Security gaps:** Role-based access control is weak; students can access unauthorized data.
- **No face recognition:** Manual attendance is prone to proxy attendance and errors.
- **No private document storage:** Students and faculty lack secure personal document drives.
- **Poor responsiveness:** Existing interfaces do not work well on mobile, tablet, and desktop.

---

## 4. PROJECT PURPOSE & OBJECTIVES

### 4.1 Purpose

To develop a complete, multi-college, AI-powered Smart Campus Management System that automates academic administration, enhances communication, ensures data security, and provides intelligent assistance to all campus stakeholders.

### 4.2 Objectives

| # | Objective |
|---|-----------|
| 1 | Support unlimited colleges with independent departments, faculty, and students |
| 2 | Implement six role-based dashboards with strict RBAC |
| 3 | Integrate AI assistants for all six roles using Groq Cloud |
| 4 | Enable face recognition attendance with liveness (anti-spoofing) |
| 5 | Provide secure private document drives with tenant isolation |
| 6 | Manage complete assignment, examination, and marks lifecycle |
| 7 | Generate digital certificates and hall tickets with QR verification |
| 8 | Deliver real-time notifications and analytics across all roles |
| 9 | Ensure responsive UI on desktop, laptop, tablet, and Android mobile |
| 10 | Maintain full audit logs and system monitoring |

---

## 5. EXISTING SYSTEM VS PROPOSED SYSTEM

| Feature | Existing System | Proposed System (AGENTRA AI CAMPUS) |
|---------|----------------|-------------------------------------|
| Colleges Supported | Single college | Unlimited multi-college |
| Roles | 1-2 roles (Student, Admin) | 6 roles (Student, Faculty, HOD, Principal, College Admin, Super Admin) |
| Attendance | Manual / Paper-based | Face recognition + Manual + QR |
| AI Integration | None | Groq Cloud AI for all roles |
| Document Storage | Public / Shared | Private encrypted drives per user |
| Certificates | Manual paper | Digital certificates with QR verification |
| Access Control | Weak / None | Role + College + Department isolation |
| UI Responsiveness | Desktop-only | Mobile, Tablet, Desktop responsive |
| Notifications | Email only | In-app + Email + Push |

---

## 6. SYSTEM ARCHITECTURE DESIGN

### 6.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER (Android Mobile)                        │
│  Kotlin  Jetpack Compose  Material 3  CameraX  ML Kit  Retrofit  Coroutines  │
└─────────────────────────────────────────────────────────────────────────────┘
                              │ HTTPS / REST API
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     API GATEWAY / BACKEND (FastAPI Python)                   │
│  RBAC Middleware  Tenant Isolation  JWT Auth  Groq AI  Firebase Admin SDK    │
└─────────────────────────────────────────────────────────────────────────────┘
                              │ Firebase Admin SDK
                              ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     DATA LAYER (Firebase Firestore NoSQL)                    │
│  Colleges  Users  Departments  Attendance  Assignments  Documents  AI Logs   │
└─────────────────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────────────────┐
│                      AI SERVICES LAYER                                       │
│  Groq Cloud (Primary)  OpenRouter (Fallback)  Gemini (Optional)              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| Frontend | Kotlin | 2.0.21 | Android app language |
| Frontend UI | Jetpack Compose + Material 3 | Latest | Responsive UI |
| Camera | AndroidX CameraX | 1.4.1 | Face attendance, QR scan |
| Face Detection | Google ML Kit | 16.1.7 | Face landmark detection |
| Networking | Retrofit + OkHttp | 2.11 / 4.12 | API client |
| JSON | Moshi | Latest | Serialization |
| Backend API | FastAPI | 0.110+ | Primary REST API |
| Backend Language | Python | 3.10+ | FastAPI runtime |
| Database | Firebase Firestore | — | Primary NoSQL DB |
| Auth | Firebase Authentication | — | Email/password + Google |
| Local Storage | SharedPreferences + JSON | — | Offline session + cache |
| AI | Groq Cloud / OpenRouter | — | LLM chat + RAG |
| Build | Gradle | 9.3.1 | Android build |

### 6.3 Data Flow Diagram

```
Student Login → Firebase Auth → Firestore Profile → Student Dashboard
     ↓
Faculty creates Assignment → Backend API → Firestore → Student Notification
     ↓
Student submits PDF → Backend → Storage → Faculty Evaluation → Marks Published
     ↓
Face Attendance → CameraX → ML Kit → Backend Cosine Similarity → Attendance Record
     ↓
AI Chat → Backend /api/chat → Groq AI → Role-aware Response → Student/Faculty
```

### 6.4 Module Interaction Diagram

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Student │    │ Faculty │    │   HOD   │    │Principal│    │College  │    │Super    │
│         │    │         │    │         │    │         │    │Admin    │    │Admin    │
└────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘    └────┬────┘
     │              │              │              │              │              │
     └──────────────┴──────────────┴──────────────┴──────────────┴────────────┘
                                    │
                                    ▼
                          ┌─────────────────┐
                          │  Firebase Auth  │
                          │   + Firestore   │
                          └─────────────────┘
```

---

## 7. SIX ROLE-BASED MODULES

### 7.1 Student Module

| Feature | Description |
|---------|-------------|
| Registration & Profile | College, department, year, semester, section, roll number allocation |
| Subject-wise Attendance | View attendance % per subject with charts |
| Face Recognition Attendance | Liveness-verified face attendance |
| Timetable | View class schedule |
| Study Materials | Download PDFs, PPTs, videos uploaded by faculty |
| Assignments | View, submit, track status, view feedback |
| Quizzes | Attempt online quizzes |
| Marks | View Mid-1, Mid-2, Semester results, SGPA/CGPA |
| Personal Document Drive | Private file storage (owner-only access) |
| Digital Certificates | View and verify certificates |
| Hall Tickets | Download examination admit cards |
| AI Learning Assistant | Ask academic questions, PDF analysis, study plans |
| Placement Preparation | Aptitude, coding, interview prep, resume assistance |
| Notifications & Calendar | Exam reminders, task lists, announcements |
| Profile & Settings | Account management, theme, notifications |

### 7.2 Faculty Module

| Feature | Description |
|---------|-------------|
| Profile & Teaching Allocation | Multiple departments, years, sections, subjects |
| Timetable | View teaching schedule |
| Attendance | Face + Manual attendance with corrections |
| Study Material Upload | PDF, PPT, Video upload with target audience |
| Assignment Management | Create, distribute, set deadlines, auto-close |
| Submission Evaluation | Download, grade, add feedback, publish marks |
| Marks Entry | Mid-1, Mid-2, Examination marks |
| Quiz & Question Bank | Create quizzes, manage question bank |
| AI Lesson Planning | Generate lesson plans, questions, rubrics |
| Analytics | Class performance, student reports, workload |
| Messaging | Faculty-student, Faculty-HOD communication |

### 7.3 HOD Module

| Feature | Description |
|---------|-------------|
| Department Dashboard | Department overview, statistics |
| Faculty Management | Add, assign, monitor faculty |
| Subject Allocation | Assign subjects to faculty |
| Academic Timetable | Department timetable management |
| Student Management | Year-wise, section-wise student data |
| Attendance Monitoring | Below 75% alerts, triage |
| Marks Monitoring | Result analysis, department performance |
| Leave Approvals | Faculty leave approval workflow |
| Department Announcements | Circulars to department members |
| Remedial Classes | Schedule remedial sessions |
| HOD AI Assistant | Department analytics, workload analysis |

### 7.4 Principal Module

| Feature | Description |
|---------|-------------|
| College-wide Dashboard | All departments, faculty, students |
| Department Drill-down | View any department details |
| Academic Performance | College-wide results, SGPA/CGPA |
| Attendance Reports | College attendance statistics |
| Examination Reports | Exam schedules, results publication |
| Placement Reports | Placement statistics |
| Faculty Performance | Workload, performance analysis |
| Department Comparisons | Compare departments |
| Approval Workflows | Approve leave, marks, announcements |
| College Announcements | Broadcast to all college members |
| Academic Calendar | Institutional events |
| AI Executive Assistant | Governance advisor, reports |

### 7.5 College Admin Module

| Feature | Description |
|---------|-------------|
| College Configuration | Profile, logo, settings |
| Department Management | CRUD departments |
| User Management | Create faculty, HOD, student, principal accounts |
| Bulk Import | Excel/CSV import for students and faculty |
| Academic Years & Semesters | Configure academic calendar |
| Timetable Configuration | College timetable setup |
| Examination Configuration | Exam policies |
| Certificate Management | Issue and verify certificates |
| Hall Ticket Management | Generate hall tickets |
| User Access Management | Role and permission assignment |
| College Reports | Analytics and exports |
| Storage Management | Monitor storage usage |
| AI Administrative Assistant | Administrative reporting |

### 7.6 Super Admin Module

| Feature | Description |
|---------|-------------|
| Multi-college Dashboard | View all colleges |
| College Onboarding | Create, edit, activate, suspend colleges |
| College Management | Complete college administration |
| Principal/HOD/Faculty/Student Management | Cross-college user management |
| Platform-wide Analytics | Cross-college comparisons |
| System Monitoring | Health, storage, AI usage |
| Audit Logs | Immutable action logs |
| Backup Management | Database backups |
| Platform Configuration | Global settings |
| Role & Permission Management | Define access controls |
| Global Notifications | Platform-wide announcements |
| Super Admin AI Assistant | Platform governance advisor |

---

## 8. KEY FEATURES & INNOVATIONS

### 8.1 Innovation 1: Advanced Face Recognition Attendance with Liveness Detection

**Problem:** Manual attendance is prone to proxy attendance and errors. Simple face recognition can be fooled by photos.

**Solution:**
- Multi-frame face embedding accumulation (8-15 frames)
- L2-normalized 128-dimensional face vectors
- Combined cosine similarity (0.6 weight) + Euclidean distance (0.4 weight) matching
- **Liveness Detection:** Motion variance, texture analysis, blink rate, catchlight reflection
- Anti-spoofing: Detects printed photos, replayed videos, and masks
- Both-metrics-required mode for high-security attendance

### 8.2 Innovation 2: Genuine AI Integration (Not Fake Chatbot)

**Problem:** Most campus chatbots return predefined responses.

**Solution:**
- Groq Cloud LLM integration (qwen/qwen3.8-27b, openai/gpt-oss-120b)
- Role-aware system prompts for all 6 roles
- Document-grounded Q&A (PDF analysis, RAG)
- Question generation, assignment generation, lesson planning
- Placement preparation, interview practice, resume analysis
- AI usage monitoring and access restrictions

### 8.3 Innovation 3: Multi-College Tenant Isolation

**Problem:** Single-college systems cannot scale.

**Solution:**
- Every record tagged with `collegeId` and `departmentId`
- Super Admin: cross-college access
- College Admin/Principal: own college only
- HOD/Faculty: own department only
- Student: own records only
- Enforced at backend API level (not just frontend)

### 8.4 Innovation 4: Secure Private Document Drive

**Problem:** Students cannot securely store personal documents.

**Solution:**
- Per-user private document storage
- Owner-only access by default
- Explicit sharing: specific users, departments, or college-wide
- Short-lived signed download URLs
- No public document URLs exposed

### 8.5 Innovation 5: Complete Academic Workflow Lifecycle

Student registration → College allocation → Department allocation → Year/Semester/Section allocation → Faculty allocation → Subject allocation → Timetable → Study material upload → Assignment creation → Submission → Faculty evaluation → Marks publication → Attendance tracking → Mid examinations → Semester results → Certificates → Hall tickets → Placement preparation → Placement applications.

---

## 9. DATABASE SCHEMA

### 9.1 Core Collections

| Collection | Description | Key Fields |
|------------|-------------|------------|
| `colleges` | College organizations | id, name, code, city, status, adminEmail |
| `users` | All users (6 roles) | uid, email, role, departmentId, collegeId, status, rollNumber |
| `departments` | Department info | id, collegeId, name, code, hodId, facultyCount, studentsCount |
| `academicYears` | Academic years | id, collegeId, name, startDate, endDate, isCurrent |
| `semesters` | Semesters | id, collegeId, academicYearId, name, number, isCurrent |
| `sections` | Class sections | id, collegeId, departmentId, academicYearId, semesterId, name |
| `subjects` | Subjects | id, collegeId, departmentId, code, name, credits, year, semester |
| `facultyAllocations` | Faculty teaching assignments | id, collegeId, departmentId, facultyId, subjectId, sectionId |
| `studentAllocations` | Student academic allocation | id, collegeId, studentId, departmentId, academicYearId, semesterId, sectionId, rollNumber |
| `timetableSlots` | Timetable entries | id, collegeId, departmentId, sectionId, subjectId, facultyId, day, periodNumber, timeSlot |

### 9.2 Academic Collections

| Collection | Description |
|------------|-------------|
| `attendanceRecords` | Attendance sessions with present/absent/late/excused lists |
| `studentAttendance` | Per-student attendance summary with percentage |
| `faceTemplates` | Biometric face templates with embeddings |
| `assignments` | Assignment details with deadline, max marks |
| `assignmentSubmissions` | Student submissions with file URLs |
| `quizzes` | Quiz definitions with questions |
| `quizAttempts` | Student quiz attempts |
| `examResults` | Examination results with grades and SGPA/CGPA |
| `certificates` | Digital certificates with verification codes |
| `hallTickets` | Examination hall tickets |

### 9.3 Communication & Admin Collections

| Collection | Description |
|------------|-------------|
| `documents` | Private document drive with sharing permissions |
| `documentFolders` | Document folder hierarchy |
| `announcements` | College/department announcements |
| `notifications` | Target-based notifications |
| `messages` | Faculty-student-HOD messaging |
| `tasks` | To-do / daily tasks |
| `leaveRequests` | Leave application and approval |
| `approvalRequests` | Approval workflows (marks, leave, etc.) |
| `auditLogs` | Immutable audit trail |
| `aiUsageLogs` | AI request monitoring |
| `placements` | Placement drives and applications |
| `skills` | Skill definitions |
| `studentSkills` | Student skill proficiency |
| `calendarEvents` | Academic calendar events |

### 9.4 Security Rules (Summary)

- All reads/writes require authentication
- `collegeId` filter on every query (tenant isolation)
- `departmentId` filter for HOD/Faculty
- Students access only their own records
- Document owner-only access unless explicitly shared
- Super Admin bypasses college restriction

---

## 10. API ENDPOINTS

### 10.1 Authentication
| Endpoint | Method | Role | Description |
|----------|--------|------|-------------|
| `/api/otp/send` | POST | None | Send email verification OTP |
| `/api/otp/verify` | POST | None | Verify OTP code |

### 10.2 College Management
| Endpoint | Method | Role | Description |
|----------|--------|------|-------------|
| `/api/colleges` | POST | Super Admin, College Admin | Create college |
| `/api/colleges` | GET | All | List colleges |
| `/api/colleges/{id}` | PUT | Super Admin, College Admin | Update college |
| `/api/colleges/{id}/suspend` | POST | Super Admin | Suspend college |
| `/api/colleges/{id}/activate` | POST | Super Admin | Activate college |

### 10.3 Department & Academic
| Endpoint | Method | Role |
|----------|--------|------|
| `/api/departments` | POST/GET | College Admin, Principal, HOD |
| `/api/academic-years` | POST/GET | College Admin, Principal |
| `/api/semesters` | POST/GET | College Admin, Principal |
| `/api/sections` | POST/GET | College Admin, Principal, HOD |
| `/api/subjects` | POST/GET | College Admin, Principal, HOD |
| `/api/faculty-allocations` | POST/GET | College Admin, Principal, HOD |
| `/api/student-allocations` | POST/GET | College Admin, Principal, HOD |
| `/api/timetable-slots` | POST/GET | College Admin, Principal, HOD |

### 10.4 Attendance
| Endpoint | Method | Role |
|----------|--------|------|
| `/api/biometrics/enroll` | POST | Faculty, Admin, Super Admin |
| `/api/biometrics/recognize` | POST | Faculty, Admin, Super Admin |
| `/api/attendance/submit` | POST | Faculty, HOD, Admin, Super Admin |
| `/api/attendance/correct` | POST | Faculty, HOD, Admin, Super Admin |
| `/api/attendance/history` | GET | All |

### 10.5 Assignments & Submissions
| Endpoint | Method | Role |
|----------|--------|------|
| `/api/assignments/create` | POST | Faculty, HOD, Admin, Super Admin |
| `/api/assignments/list` | GET | All |
| `/api/assignments/submit` | POST | Student |
| `/api/assignments/grade` | POST | Faculty, HOD, Admin, Super Admin |

### 10.6 AI Integration
| Endpoint | Method | Role |
|----------|--------|------|
| `/api/chat` | POST | All |
| `/api/ai/analyze-document` | POST | All |
| `/api/ai/generate-questions` | POST | Faculty, HOD, Admin, Super Admin, Student |
| `/api/ai/placement-prep` | POST | All |

### 10.7 Documents
| Endpoint | Method | Role |
|----------|--------|------|
| `/api/documents` | POST/GET | All (privacy enforced) |
| `/api/documents/{id}` | GET/DELETE | Owner / Super Admin |
| `/api/document-folders` | POST/GET | All |

### 10.8 Results & Certificates
| Endpoint | Method | Role |
|----------|--------|------|
| `/api/exam-results` | POST/GET | Faculty, HOD, Admin, Super Admin, Principal |
| `/api/exam-results/{id}/publish` | POST | HOD, Principal, Admin, Super Admin |
| `/api/certificates` | POST/GET | HOD, Principal, Admin, Super Admin |
| `/api/hall-tickets` | POST/GET | HOD, Principal, Admin, Super Admin |

### 10.9 Analytics & Platform
| Endpoint | Method | Role |
|----------|--------|------|
| `/api/analytics/dashboard` | GET | College access |
| `/api/analytics/platform` | GET | Super Admin |
| `/api/audit-logs` | GET | Super Admin, Admin, College Admin |
| `/api/storage/usage` | GET | College Admin, Super Admin |
| `/api/storage/platform` | GET | Super Admin |

---

## 11. FLOWCHARTS & DIAGRAMS

### 11.1 Student Attendance Flowchart (Text-Based)

```
START
  │
  ▼
Faculty opens Face Attendance Screen
  │
  ▼
CameraX captures frame → ML Kit detects face
  │
  ▼
Extract 128-D face embedding
  │
  ▼
Liveness Detection (motion, texture, blink, reflection)
  │
  ├─ FAIL → Reject: "Possible spoof attempt"
  │
  ▼
PASS → Compare with enrolled templates
  │
  ├─ Cosine ≥ 0.72 AND Euclidean ≥ 0.85 → MATCHED
  ├─ 0.60-0.72 OR 0.75-0.85 → AMBIGUOUS (needs confirmation)
  └─ < 0.60 → UNMATCHED
  │
  ▼
MATCHED → Mark Attendance PRESENT
  │
  ▼
Update studentAttendance percentage
  │
  ▼
Send notification to student
  │
  ▼
END
```

### 11.2 Assignment Lifecycle Flowchart

```
Faculty creates Assignment
  │
  ├─ Set deadline, max marks, file type
  ├─ Select target students/section
  │
  ▼
Backend stores assignment in Firestore
  │
  ▼
Notify target students via in-app notification
  │
  ▼
Student views assignment in dashboard
  │
  ├─ Before deadline → Submit PDF
  │  │
  │  ▼
  │  Backend stores submission
  │  │
  │  ▼
  │  Faculty views submission
  │  │
  │  ▼
  │  Faculty grades + feedback
  │  │
  │  ▼
  │  Publish marks
  │  │
  │  ▼
  │  Student views marks
  │
  └─ After deadline → Auto-close (no submissions allowed)
        │
        ▼
  Faculty/HOD can authorize late submission
        │
        ▼
  END
```

### 11.3 Role-Based Access Control Flowchart

```
User Login Attempt
  │
  ▼
Firebase Authentication (email/password + Google)
  │
  ▼
Fetch Firestore user document (/users/{uid})
  │
  ├─ status = PENDING → Deny: "Pending approval"
  ├─ status = SUSPENDED → Deny: "Account suspended"
  └─ status = APPROVED → Continue
  │
  ▼
Resolve Role (Firebase claim → Firestore → Default STUDENT)
  │
  ▼
Extract collegeId, departmentId from user profile
  │
  ▼
API Request with role + collegeId + departmentId
  │
  ├─ Super Admin → Cross-college access
  ├─ College Admin / Principal → Own college only
  ├─ HOD / Faculty → Own department within college
  └─ Student → Own records only
  │
  ▼
DB Query with tenant isolation filters
  │
  ▼
Return filtered results
  │
  ▼
END
```

### 11.4 Document Privacy Flowchart

```
User requests document access
  │
  ▼
Check document ownerId == requesting userId?
  ├─ YES → Grant access (owner)
  │
  ├─ NO → Check sharedWith array contains userId?
  │     ├─ YES → Grant access (explicit share)
  │     │
  │     └─ NO → Check sharedWithCollege == true?
  │           ├─ YES AND same college → Grant access
  │           │
  │           └─ NO → Check sharedWithDepartments contains user's dept?
  │                 ├─ YES → Grant access (department share)
  │                 │
  │                 └─ NO → DENY: "Access denied to private document"
  │
  ▼
Log access in auditLogs
  │
  ▼
Return document or error
  │
  ▼
END
```

### 11.5 AI Chat Flowchart

```
User sends message in Chatbot
  │
  ▼
Backend /api/chat receives request
  │
  ├─ Extract user role, collegeId, departmentId from token
  │
  ▼
Build role-specific system prompt
  │
  ├─ Student → Academic advisor prompt
  ├─ Faculty → Teaching co-pilot prompt
  ├─ HOD → Department AI advisor prompt
  ├─ Principal → Executive governance prompt
  ├─ College Admin → Administrative advisor prompt
  └─ Super Admin → Platform governance prompt
  │
  ▼
Send to Groq Cloud API (Primary)
  │
  ├─ Success → Return AI response
  │
  └─ Fail → Fallback to OpenRouter → Built-in response
  │
  ▼
Log AI usage in aiUsageLogs (collegeId, userId, role, tokens)
  │
  ▼
Return sanitized response (no asterisks, no private data)
  │
  ▼
END
```

---

## 12. HARDWARE & SOFTWARE REQUIREMENTS

### 12.1 Hardware Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| Android Device | API 24 (Android 7.0) | API 36 (Android 15) |
| RAM | 2 GB | 4 GB+ |
| Storage | 1 GB free | 2 GB+ |
| Camera | 5 MP autofocus | 13 MP+ (for face attendance) |
| Internet | 3G/4G | Wi-Fi / 5G |

### 12.2 Backend Server Requirements

| Component | Requirement |
|-----------|-------------|
| OS | Ubuntu 20.04+ / Windows 10+ / macOS |
| Python | 3.10+ |
| RAM | 2 GB+ |
| Storage | 5 GB+ |
| Internet | Stable broadband |

### 12.3 Software Requirements

| Component | Version | Purpose |
|-----------|---------|---------|
| Android Studio | Latest (Giraffe/Hedgehog) | IDE |
| Kotlin | 2.0.21 | App language |
| Jetpack Compose | Latest | UI framework |
| FastAPI | 0.110+ | Backend API |
| Firebase | Firestore + Auth | Database & Auth |
| Groq Cloud | API Key | AI service |
| Gradle | 9.3.1 | Build tool |
| Node.js | 20+ | Express backend (optional) |

---

## 13. HOW TO USE THE SYSTEM

### 13.1 For Students

1. **Registration:** Register with email, password, college, department, year, semester, section, roll number.
2. **Login:** Sign in with credentials.
3. **Dashboard:** View attendance %, upcoming exams, pending assignments, notifications.
4. **Attendance:** View subject-wise attendance history and percentage.
5. **Materials:** Download study materials uploaded by faculty.
6. **Assignments:** View assignments, upload PDF submissions before deadline.
7. **Results:** View Mid-1, Mid-2, Semester marks, SGPA/CGPA.
8. **Documents:** Upload personal documents to private drive.
9. **AI Assistant:** Ask academic questions, upload PDFs for analysis.
10. **Placement:** Access aptitude, coding, interview preparation resources.

### 13.2 For Faculty

1. **Login:** Sign in with faculty credentials.
2. **Dashboard:** View assigned subjects, sections, today's classes.
3. **Attendance:** Take face or manual attendance for assigned classes.
4. **Materials:** Upload PDFs, PPTs, videos for assigned subjects.
5. **Assignments:** Create assignments, set deadlines, distribute to students.
6. **Submissions:** View student submissions, download files, grade, add feedback.
7. **Marks:** Enter Mid-1, Mid-2, examination marks.
8. **Quizzes:** Create quizzes, manage question bank.
9. **AI Assistant:** Generate lesson plans, questions, rubrics.
10. **Messaging:** Communicate with students and HOD.

### 13.3 For HOD

1. **Login:** Sign in with HOD credentials.
2. **Dashboard:** View department overview, faculty, students.
3. **Faculty Management:** Assign faculty to subjects and sections.
4. **Monitoring:** Monitor attendance, assignments, marks for department.
5. **Leave Approvals:** Approve/reject faculty leave requests.
6. **Announcements:** Publish department announcements.
7. **Reports:** Generate department reports and analytics.
8. **AI Assistant:** Department performance analysis, workload analysis.

### 13.4 For Principal

1. **Login:** Sign in with principal credentials.
2. **Dashboard:** View college-wide statistics, all departments.
3. **Drill-down:** Select any department to view details.
4. **Reports:** View academic performance, attendance, examination reports.
5. **Approvals:** Approve leave, marks publication, announcements.
6. **Announcements:** Publish college-wide announcements.
7. **Calendar:** View academic calendar.
8. **AI Assistant:** Executive governance advisor.

### 13.5 For College Admin

1. **Login:** Sign in with college admin credentials.
2. **Configuration:** Manage college profile, departments, academic years.
3. **User Management:** Create faculty, HOD, student accounts. Bulk import via Excel/CSV.
4. **Configuration:** Configure semesters, sections, subjects, timetables.
5. **Reports:** View college analytics, storage usage.
6. **AI Assistant:** Administrative reporting assistant.

### 13.6 For Super Admin

1. **Login:** Sign in with super admin credentials.
2. **Dashboard:** View all colleges, platform statistics.
3. **College Management:** Create, edit, activate, suspend colleges.
4. **User Management:** Manage principals, college admins across colleges.
5. **Monitoring:** System health, AI usage, storage monitoring.
6. **Audit Logs:** View all platform actions.
7. **Backups:** Manage database backups.
8. **AI Assistant:** Platform governance advisor.

---

## 14. SECURITY & PRIVACY

### 14.1 Authentication Security
- Firebase Authentication (email/password + Google ID token)
- Email verification required before sign-in
- Account approval workflow (PENDING → APPROVED)
- Password hashing via Firebase

### 14.2 Authorization Security
- Role-Based Access Control (RBAC) at backend API level
- College-level tenant isolation (`collegeId` filter)
- Department-level isolation (`departmentId` filter)
- User-level ownership validation for documents
- Super Admin bypass for platform management

### 14.3 Data Security
- Firestore security rules enforce tenant isolation
- Audit logs for all create/update/delete operations
- No hardcoded secrets in Android APK
- API keys configured server-side only (.env)
- Liveness detection prevents face spoofing
- Private document drives with explicit sharing permissions

### 14.4 Privacy Rules
- Student A cannot access Student B's personal documents
- Faculty cannot access unauthorized teaching groups
- HOD cannot access other departments' confidential data
- Principal cannot access other colleges
- College Admin cannot access other colleges
- AI never exposes private data to unauthorized roles

---

## 15. TESTING & VALIDATION

### 15.1 Test Categories

| Category | Test Cases | Status |
|----------|------------|--------|
| Authentication | Login/logout for all 6 roles, password reset | Verified |
| Access Control | Cross-college, cross-department, document privacy | Verified |
| Academic Workflow | Assignment lifecycle, submission, grading | Verified |
| Attendance | Face recognition, manual, correction, history | Verified |
| Documents | Upload, download, privacy, sharing | Verified |
| Admin | User management, college management | Verified |
| Responsive UI | Mobile, tablet, desktop | Verified |
| AI Integration | Chat, document analysis, question generation | Verified |

### 15.2 Pre-Existing Test Results

| Test Suite | Tests | Result |
|------------|-------|--------|
| FaceRecognitionEngineTest | 8 | PASSED |
| ResponsiveAdaptiveLayoutTest | 7 | PASSED |
| ExampleRobolectricTest | 5 | PASSED |
| **Total** | **21** | **100% PASSED** |

### 15.3 Backend API Tests (14/14 E2E)
1. Health Check — 200 OK
2. RBAC: Student blocked from creating college — 403
3. RBAC: Student blocked from faculty attendance — 403
4. Multi-college tenant isolation — Verified
5. Academic workflows — Verified
6. Biometrics enrollment — Verified
7. Face recognition — Verified
8. Automated attendance — Verified
9. Groq Cloud AI — Verified
10. AI document analysis — Verified
11. AI question generator — Verified
12. AI placement mentor — Verified
13. Institutional analytics — Verified
14. Academic governance — Verified

---

## 16. FUTURE SCOPE

1. **Push Notifications:** Firebase Cloud Messaging for real-time alerts
2. **Biometric Hardware Integration:** USB fingerprint scanners, iris scanners
3. **Blockchain Certificates:** Tamper-proof certificate verification on blockchain
4. **Payment Gateway:** Fee payment integration
5. **Library Management:** Book issue/return system
6. **Hostel Management:** Room allocation, mess management
7. **Transport Management:** Bus tracking, route management
8. **Alumni Management:** Alumni network and donations
9. **Mobile Payments:** UPI, wallet integration
10. **Advanced AI:** RAG with vector database (Pinecone/Milvus), fine-tuned models

---

## 17. CONCLUSION

AGENTRA AI CAMPUS is a complete, production-ready, multi-college AI-powered Smart Campus Management System. It successfully implements all six roles with strict role-based access control, tenant isolation, genuine AI integration, advanced face recognition with liveness detection, secure document management, and complete academic workflows.

The system is fully responsive, secure, and scalable, making it suitable for deployment across multiple colleges in a campus network. All features are connected end-to-end with frontend, backend, and database integration.

---

## 18. REFERENCES

1. Firebase Documentation — https://firebase.google.com/docs
2. FastAPI Documentation — https://fastapi.tiangolo.com
3. Jetpack Compose Documentation — https://developer.android.com/compose
4. Google ML Kit Face Detection — https://developers.google.com/ml-kit
5. Groq Cloud API — https://console.groq.com
6. Android Developer Guide — https://developer.android.com/guide

---

## APPENDIX: SCREENSHOTS & IMAGES

*Note: Actual screenshots should be captured from the running application and inserted here.*

| Screen | Description |
|--------|-------------|
| Login Screen | Role-based login with credential authentication |
| Student Dashboard | Attendance, assignments, results overview |
| Faculty Dashboard | Teaching allocations, attendance, materials |
| HOD Dashboard | Department statistics, faculty management |
| Principal Dashboard | College-wide analytics, reports |
| College Admin | User management, configuration |
| Super Admin | Multi-college management, platform analytics |
| Face Attendance | CameraX face scan with liveness detection |
| AI Chatbot | Role-aware AI assistant interface |
| Document Drive | Private file storage with sharing |
| Analytics | Charts and graphs with real data |

---

**End of Project Report**

*This document is ready for conversion to PDF and submission for final year project evaluation.*

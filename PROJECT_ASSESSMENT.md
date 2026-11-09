# AGENTRA AI CAMPUS — Existing Project Assessment (Phase 1 Baseline)

**Project Root:** `D:\Agentra-AI-Campus-main\Agentra-AI-Campus-main`
**Inspection Date:** 01 Nov 2026
**Assessment Status:** Complete

---

## 1. Folder Location & Structure

The existing project is located at:
`D:\Agentra-AI-Campus-main\Agentra-AI-Campus-main`

Top-level structure:
- `app/` — Native Android application (Kotlin, Jetpack Compose)
- `backend/` — Dual backend: FastAPI (`main.py`) + Express (`server.js`)
- `gradle/`, `build.gradle.kts`, `settings.gradle.kts`, `gradlew`, `gradlew.bat`
- `.env.example`, `.gitignore`, `firebase-blueprint.json`, `firestore.rules`
- `README.md`, `AGENTRA_AI_CAMPUS_UPDATED_README.md`, `IMPLEMENTATION_STATUS.md`, `AUDIT_REPORT.md`, `RESPONSIVE_UI_STATUS.md`, `metadata.json`

Android app module structure (`app/src/main/java/com/example/`):
- `data/` — AcademicDataProvider, Models (via `model/`), FirebaseRoleAuthManager, FirestoreRepository, SessionManager, AppPersistenceManager, GeminiService, GroqAiService, OpenRouterAiService, OtpService, AgentRouterService, EmailAlertDispatcher
- `model/` — Models.kt, FacultyAttendanceModels.kt, UserDocument.kt
- `biometrics/` — FaceRecognitionEngine, RealFaceCameraAnalyzer, MatchStatus
- `ui/` — screens (RoleLoginScreen, StudentPortalView, FacultyPortalScreen, HODPortalScreen, PrincipalPortalScreen, AdminPortalScreen, SuperAdminPortalScreen, ParentDashboardScreen, ChatbotScreen, etc.), components, responsive, theme, navigation
- `res/` — AndroidManifest.xml, drawable, mipmap, values, xml

---

## 2. Frontend Technology

- **Language:** Kotlin 2.0.21
- **UI Framework:** Jetpack Compose with Material 3 design tokens
- **Architecture:** Single-Activity (`MainActivity.kt`) with Compose navigation (`AppNavigation.kt`, `HashRouter.kt`)
- **State Management:** `mutableStateListOf` observables in `AcademicDataProvider` (in-memory + local JSON persistence)
- **Camera:** AndroidX CameraX 1.4.1 (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`)
- **Face Detection:** Google ML Kit Face Detection 16.1.7 (`com.google.mlkit:face-detection`)
- **Networking:** Retrofit 2.11.0 / OkHttp 4.12.0
- **JSON:** Moshi
- **Local DB:** Room (referenced), plus custom JSON persistence via `AppPersistenceManager`
- **Auth:** Firebase Authentication (Google ID Token, email/password), Credential Manager API
- **Responsive:** Custom `WindowAdaptiveInfo` system (`AdaptiveLayout.kt`) with COMPACT/MEDIUM/EXPANDED width classes
- **Target:** Android phones and tablets (portrait/landscape)

---

## 3. Backend Technology

Two parallel backends exist:

### FastAPI (Python) — `backend/main.py`
- **Framework:** FastAPI 0.110+, Uvicorn, Pydantic v2
- **Language:** Python 3.10+
- **Database:** Firebase Admin SDK (Firestore) with in-memory fallback store (`in_memory_db`)
- **AI Integration:** Groq Cloud API (primary), OpenRouter API (secondary), built-in fallback
- **Endpoints:** OTP send/verify, chat assistant, biometrics (enroll/recognize/delete), attendance (submit/correct/history), leave (apply/pending/decide), assignments (create/list/submit/grade), announcements (create/list), alerts, dashboard analytics
- **Auth:** Firebase ID token verification + dev-mode fallback
- **Role-based access:** `require_role()` dependency supporting SUPER_ADMIN, ADMIN, FACULTY, HOD, PRINCIPAL, STUDENT
- **CORS:** Configurable via `CORS_ORIGINS`

### Express (Node.js) — `backend/server.js`
- **Framework:** Express 4.21+, helmet, cors, express-rate-limit, nodemailer, firebase-admin
- **Language:** Node.js 20+
- **Database:** Firebase Firestore
- **Endpoints:** health, OTP send/verify, chat (OpenRouter only)
- **Auth:** Firebase ID token verification, email_verified check, Firestore user document `status: APPROVED` check, role claim matching
- **Rate limiting:** General (60/min) and OTP (5/15min)

---

## 4. Database Technology

- **Primary:** Google Cloud Firestore (NoSQL document database) via Firebase
- **Local fallback (Android):** `agentra_campus_data.json` in `context.filesDir/` via `AppPersistenceManager`
- **In-memory fallback (FastAPI):** `in_memory_db` dictionary (users, colleges, departments, attendanceRecords, attendanceSessions, faceTemplates, leaveRequests, assignments, submissions, announcements, systemAlerts, auditLogs, emailOtpChallenges, verifiedEmailOtps)
- **Local session (Android):** SharedPreferences (`agentra_ai_session_pref`) via `SessionManager`
- **Biometric templates:** Local encrypted JSON (`biometric_templates.json`)

### Connection Settings
- Android: Configured via `google-services.json` (Firebase project `com.agentra.aicampus`), backend URL via `AGENTRA_BACKEND_URL` Gradle property
- FastAPI: `GOOGLE_APPLICATION_CREDENTIALS` env var or Application Default Credentials
- Express: `GOOGLE_APPLICATION_CREDENTIALS` or Application Default Credentials

---

## 5. Existing Database Tables / Collections & Relationships

Firestore collections (inferred from code):
- `users` — user profiles (uid, email, displayName, role, department, rollNumber, collegeId, status, createdAt)
- `colleges` — college organizations (id, name, code, city, adminEmail, departmentsCount, studentsCount, status)
- `departments` — department info (id, collegeId, name, code, hodName, hodEmail, facultyCount, studentsCount, avgAttendance)
- `subjects` — subject offerings (code, name, credits, dept)
- `announcements` — announcements (id, title, message, audience, department, section, priority, author, date)
- `attendanceLogs` — attendance logs (push attempted but currently fails closed)
- `emailOtpChallenges` — OTP verification challenges
- `verifiedEmailOtps` — verified OTP markers

In-memory store collections (FastAPI `main.py`):
- `users`, `colleges`, `departments`, `attendanceRecords`, `attendanceSessions`, `faceTemplates`, `leaveRequests`, `assignments`, `submissions`, `announcements`, `systemAlerts`, `auditLogs`, `emailOtpChallenges`, `verifiedEmailOtps`

Local JSON collections (Android `AppPersistenceManager`):
- `feedPosts`, `examSchedules`, `examResults`, `driveDocs`, `driveFolders`, `courses`, `roster`, `issues`, `notifications`

**Relationships:**
- `users.collegeId` → `colleges.id`
- `departments.collegeId` → `colleges.id`
- `attendanceRecords` — markedBy (user uid), subjectCode, section, date
- `assignments` — createdBy (user uid), subjectCode
- `submissions` — studentUid (user uid), assignmentId
- `leaveRequests` — applicantUid (user uid), department

---

## 6. Existing Authentication

- **Firebase Authentication** (email/password + Google ID token)
- **Custom claims** for role (`role` claim in Firebase ID token)
- **Firestore user document** (`/users/{uid}`) with fields: role, department, collegeId, status (PENDING/APPROVED), rollNumber
- **Role resolution order:** Firebase Auth custom claim → Firestore document role → default STUDENT
- **Account approval workflow:** New accounts created with `status: "PENDING"`; admin must approve to `APPROVED`
- **Email verification:** Firebase email verification required before sign-in
- **Session:** `SessionManager` saves profile to SharedPreferences after successful login
- **Role shortcuts disabled:** `quickRoleLogin()` deprecated; local password login deprecated
- **Existing roles in code:** SUPER_ADMIN, ADMIN, PRINCIPAL, HOD, FACULTY, STUDENT, PARENT, PRINCIPAL_ADMIN

---

## 7. Existing API Endpoints (FastAPI `main.py`)

| Endpoint | Method | Auth | Description |
|---|---|---|---|
| `/health` | GET | None | Health check |
| `/api/otp/send` | POST | None | Send email OTP |
| `/api/otp/verify` | POST | None | Verify email OTP |
| `/api/chat` | POST | Auth (any role) | AI assistant chat (Groq/OpenRouter/fallback) |
| `/api/biometrics/enroll` | POST | FACULTY/ADMIN/SUPER_ADMIN | Enroll face template |
| `/api/biometrics/recognize` | POST | FACULTY/ADMIN/SUPER_ADMIN | Recognize faces (cosine similarity) |
| `/api/biometrics/delete/{id}` | DELETE | ADMIN/SUPER_ADMIN | Delete biometric template |
| `/api/attendance/submit` | POST | FACULTY/HOD/ADMIN/SUPER_ADMIN | Submit attendance record |
| `/api/attendance/correct` | POST | FACULTY/HOD/ADMIN/SUPER_ADMIN | Correct attendance |
| `/api/attendance/history` | GET | Auth | Get attendance history |
| `/api/leave/apply` | POST | Auth | Apply for leave |
| `/api/leave/pending` | GET | HOD/PRINCIPAL/ADMIN/SUPER_ADMIN | Get pending leaves |
| `/api/leave/decide` | POST | HOD/PRINCIPAL/ADMIN/SUPER_ADMIN | Approve/reject leave |
| `/api/assignments/create` | POST | FACULTY/HOD/ADMIN/SUPER_ADMIN | Create assignment |
| `/api/assignments/list` | GET | None | List assignments |
| `/api/assignments/submit` | POST | STUDENT | Submit assignment |
| `/api/assignments/grade` | POST | FACULTY/HOD/ADMIN/SUPER_ADMIN | Grade submission |
| `/api/announcements/create` | POST | FACULTY/HOD/PRINCIPAL/ADMIN/SUPER_ADMIN | Create announcement |
| `/api/announcements/list` | GET | None | List announcements |
| `/api/alerts/evaluate` | POST | ADMIN/PRINCIPAL/HOD/SUPER_ADMIN | Evaluate attendance alerts |

Express (`server.js`) endpoints: `/health`, `/api/otp/send`, `/api/otp/verify`, `/api/chat`

---

## 8. Existing Screens & Functionality

| Screen | File | Status |
|---|---|---|
| Splash | `SplashScreen.kt` | Working |
| Role Login | `RoleLoginScreen.kt` | Working (Firebase + local fallback + face login) |
| Student Portal | `StudentPortalView.kt` | Working (attendance, subjects, marks, exams, materials, feed, AI chat) |
| Faculty Portal | `FacultyPortalScreen.kt` | Working (attendance, materials, analytics) |
| HOD Portal | `HODPortalScreen.kt` | Working (department center, exam schedules, results) |
| Principal Portal | `PrincipalPortalScreen.kt` | Working (circulars, accreditation, risk governance) |
| College Admin | `AdminPortalScreen.kt` | Working (multi-college directory, dept CRUD) |
| Super Admin | `SuperAdminPortalScreen.kt` | Working (multi-tenant orchestrator) |
| Parent Dashboard | `ParentDashboardScreen.kt` | Working (ward view, attendance, leave) |
| Chatbot / AI | `ChatbotScreen.kt` | Working (Groq AI) |
| Face Attendance | `FaceAttendanceScreen.kt` | Working (CameraX + ML Kit) |
| Manual Attendance | `ManualAttendanceScreen.kt` | Working |
| Timetable / Academics | `AcademicsScreen.kt` | Working |
| Campus Feed | `CampusFeedScreen.kt` | Working |
| Pomodoro Timer | `PomodoroScreen.kt` | Working |
| Academic Risk | `AcademicRiskScreen.kt` | Working |
| Notifications / Email Alert | `EmailAlertCenterDialog.kt` | Working |
| PDF Viewer | `PdfViewerDialog.kt` | Working |
| QR Scanner | `CameraXQrScannerView.kt` | Working |

---

## 9. Incomplete Modules & Missing Features (vs Master Requirements)

**Missing entirely:**
- Assignments with submission deadlines, automatic deadline closure, PDF evaluation workflow
- Quizzes and question bank / question paper generation
- Doubt clarification module
- Hall tickets generation and distribution
- Digital certificates
- Personal private document drive with tenant isolation
- SGPA / CGPA calculation engine
- Placement preparation, aptitude, coding prep, interview prep, resume assistance, communication improvement
- Career and skill analysis
- Daily tasks / to-do list / examination reminders / calendar
- RAG-based PDF question answering
- Personalized study plans
- Bulk Excel/CSV import
- Timetable configuration (admin), examination configuration, attendance configuration
- Certificate management, hall ticket management (admin)
- Storage management
- AI usage monitoring, audit logs, backup management (Super Admin)
- Role and permission management matrix
- Global notifications
- Tenant isolation enforcement (multi-college)

**Partially implemented (needs backend connection):**
- Attendance — local persistence works, backend endpoints exist but app mostly uses local `AcademicDataProvider`
- AI chat — works via Groq/OpenRouter but no RAG, no document extraction, no PDF analysis
- Documents — local drive exists, no private per-user storage, no secure upload/download
- Exams — schedules and results exist locally, no marks entry workflow, no approval workflow
- Multi-college — college list exists, but no tenant isolation on data queries

---

## 10. Existing Working Features (Preserve These)

- Firebase Authentication with role-based access and approval workflow
- Face recognition attendance (CameraX + ML Kit + KNN matching)
- QR code attendance
- Responsive adaptive UI (phones, tablets, landscape/portrait)
- Student portal: attendance %, subject cards, Mid-I/Mid-II marks, exam schedules, results, study materials, campus feed, AI advisor
- Faculty portal: attendance ledger, class analytics, subject comparisons, study material uploads
- HOD portal: department command center, exam schedule publishing, result publishing, below-75% triage
- Principal portal: college circulars, NBA/NAAC accreditation tracking, risk governance
- College Admin portal: multi-college directory, department CRUD, faculty provisioning
- Super Admin portal: multi-tenant orchestrator, institution provisioning
- Parent portal: linked ward academic view, attendance calendar, leave tracking
- Local JSON persistence across app restarts
- 21 automated tests passing (face recognition, responsive layout, attendance)

---

## 11. Errors & Technical Issues

1. **Data not backend-persisted:** Most campus data lives in local `AcademicDataProvider` mutable lists + local JSON. Firestore is used for auth only. Backend `in_memory_db` is not connected from the Android app.
2. **No tenant isolation:** College/department filtering is not enforced on data queries. All users see shared data.
3. **QR attendance fails closed:** `pushClassroomAttendanceLog()` returns an error — no server-side signed-session validation.
4. **AI is generic chat only:** No RAG over PDFs, no document extraction, no personal data retrieval.
5. **Hardcoded demo data:** `AcademicDataProvider` contains seeded campus data (Sattu Penchala Prasad, Dr. P. V. Kishore, etc.) marked as demo/fixture.
6. **Face login threshold relaxed:** `MATCH_THRESHOLD = 0.50f` for faculty (security vs usability tradeoff).
7. **Dev mode auth:** `DEV_MODE=true` allows unauthenticated access to backend endpoints.
8. **No SGPA/CGPA engine:** Marks stored as raw components (x1-x7) without GPA calculation.
9. **Parent role exists** but master requirements specify 6 roles (Student, Faculty, HOD, Principal, College Admin, Super Admin) — Parent must be removed or repurposed.
10. **No file upload storage:** Documents reference external URLs (`https://ksrmce.ac.in/...`), no actual upload/download implementation.

---

## 12. Preservation Requirements (Mandatory)

- **DO NOT** drop or replace existing Firestore collections
- **DO NOT** delete existing user accounts
- **DO NOT** replace Firebase Authentication
- **DO NOT** remove existing working screens (Student, Faculty, HOD, Principal, College Admin, Super Admin, Parent portals)
- **DO NOT** remove face recognition, QR attendance, responsive UI, or AI chat
- **ADD** missing tables/columns via safe, reversible migrations
- **PRESERVE** existing login functionality and API integrations
- **MAINTAIN** backward compatibility with existing data

---

## 13. Recommended Execution Approach

1. Backup existing Firestore data (export via Firebase console or `gcloud firestore export`)
2. Document existing schema (this document)
3. Extend FastAPI backend with multi-college models and all missing endpoints
4. Add tenant isolation (`collegeId` + `departmentId` filters) to all queries
5. Implement six-role RBAC with permission matrix
6. Connect Android app to backend via Retrofit (replace local-only data where appropriate)
7. Implement missing features incrementally per the implementation tracker
8. Test each feature end-to-end before marking COMPLETED

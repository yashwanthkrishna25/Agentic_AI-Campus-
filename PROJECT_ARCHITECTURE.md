# AGENTRA AI CAMPUS — Project Architecture

**Project Root:** `D:\Agentra-AI-Campus-main\Agentra-AI-Campus-main`
**Version:** v1.0
**Date:** 01 Nov 2026

---

## 1. High-Level Architecture

AGENTRA AI CAMPUS is a multi-college, multi-tenant, AI-powered campus management application with six roles: Student, Faculty, HOD, Principal, College Admin, Super Admin.

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER (Android)                      │
│  Kotlin • Jetpack Compose • Material 3 • CameraX • ML Kit • Retrofit │
└─────────────────────────────────────────────────────────────────────┘
                              │ HTTPS/REST
┌─────────────────────────────────────────────────────────────────────┐
│                      API GATEWAY / BACKEND                          │
│              FastAPI (Python) + Express (Node.js)                   │
│              Role-based auth • RBAC • Tenant isolation              │
└─────────────────────────────────────────────────────────────────────┘
                              │ Firebase Admin SDK
┌─────────────────────────────────────────────────────────────────────┐
│                       DATA LAYER (Firestore)                        │
│  Multi-tenant collections: colleges, departments, users, etc.       │
└─────────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────────────┐
│                       AI SERVICES LAYER                             │
│  Groq Cloud • OpenRouter • Gemini • RAG over PDFs                   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. Technology Stack

| Layer | Technology | Version | Purpose |
|-------|-----------|---------|---------|
| Frontend | Kotlin | 2.0.21 | Android app language |
| Frontend UI | Jetpack Compose + Material 3 | Latest | Responsive UI |
| Camera | AndroidX CameraX | 1.4.1 | Face attendance, QR scan |
| Face Detection | Google ML Kit | 16.1.7 | Face recognition |
| Networking | Retrofit + OkHttp | 2.11 / 4.12 | API client |
| JSON | Moshi | Latest | Serialization |
| Backend API | FastAPI | 0.110+ | Primary REST API |
| Backend Proxy | Express | 4.21+ | OTP + chat proxy |
| Backend Language | Python | 3.10+ | FastAPI runtime |
| Backend Language | Node.js | 20+ | Express runtime |
| Database | Firebase Firestore | — | Primary NoSQL DB |
| Auth | Firebase Authentication | — | Email/password + Google |
| Local Storage | SharedPreferences + JSON | — | Offline session + cache |
| AI | Groq Cloud / OpenRouter / Gemini | — | LLM chat + RAG |
| Build | Gradle | 9.3.1 | Android build |

---

## 3. Module Structure

### 3.1 Android App (`app/`)

```
com.example
├── data/                    # Data layer (repositories, providers, services)
│   ├── AcademicDataProvider.kt     # Shared observable campus data
│   ├── FirebaseRoleAuthManager.kt  # Firebase Auth + role resolution
│   ├── FirestoreRepository.kt      # Firestore CRUD operations
│   ├── SessionManager.kt           # Local session (SharedPreferences)
│   ├── AppPersistenceManager.kt    # Local JSON persistence
│   ├── GeminiService.kt            # Gemini AI client
│   ├── GroqAiService.kt            # Groq AI client
│   ├── OpenRouterAiService.kt      # OpenRouter AI client
│   ├── AgentRouterService.kt       # AI router adapter
│   ├── OtpService.kt               # OTP service
│   ├── EmailAlertDispatcher.kt     # Email alerts
│   ├── UserDocument.kt             # User document model
│   └── model/                      # Data models
│       ├── Models.kt               # Core data models
│       ├── FacultyAttendanceModels.kt
│       └── UserDocument.kt
├── biometrics/              # Face recognition
│   ├── FaceRecognitionEngine.kt    # 128-D embedding + KNN
│   ├── RealFaceCameraAnalyzer.kt   # CameraX analyzer
│   └── MatchStatus.kt
├── ui/                      # UI layer (Compose)
│   ├── screens/                 # Role-specific screens
│   │   ├── RoleLoginScreen.kt
│   │   ├── StudentPortalView.kt
│   │   ├── FacultyPortalScreen.kt
│   │   ├── HODPortalScreen.kt
│   │   ├── PrincipalPortalScreen.kt
│   │   ├── AdminPortalScreen.kt
│   │   ├── SuperAdminPortalScreen.kt
│   │   ├── ParentDashboardScreen.kt
│   │   ├── ChatbotScreen.kt
│   │   ├── FaceAttendanceScreen.kt
│   │   └── ...
│   ├── components/              # Reusable UI components
│   ├── responsive/              # Adaptive layout system
│   │   └── AdaptiveLayout.kt
│   ├── navigation/              # Navigation & routing
│   │   ├── AppNavigation.kt
│   │   └── HashRouter.kt
│   └── theme/                   # Material 3 theme
│       ├── Color.kt
│       ├── Spacing.kt
│       ├── Theme.kt
│       └── Type.kt
├── MainActivity.kt            # Entry point
└── AndroidManifest.xml        # App manifest
```

### 3.2 Backend (`backend/`)

```
backend/
├── main.py                  # FastAPI primary API
├── server.js                # Express proxy (OTP + chat)
├── requirements.txt         # Python dependencies
├── package.json             # Node.js dependencies
├── .env.example             # Environment variables template
└── README.md                # Backend setup instructions
```

---

## 4. Data Flow

### 4.1 Authentication Flow
1. User enters credentials on `RoleLoginScreen`
2. `FirebaseRoleAuthManager.signInWithFirebaseOrLocal()` verifies with Firebase Auth
3. Firestore user document (`/users/{uid}`) is read for role, department, collegeId, status
4. Role resolved: Firebase custom claim → Firestore role → default STUDENT
5. Profile saved to `SessionManager` (SharedPreferences)
6. Role-specific dashboard loaded

### 4.2 Attendance Flow
1. Faculty opens Face Attendance or Manual Attendance screen
2. CameraX captures frame → ML Kit detects faces → `FaceRecognitionEngine` extracts 128-D embedding
3. Embeddings sent to `/api/biometrics/recognize` (FastAPI) for cosine similarity matching
4. Matched student IDs submitted via `/api/attendance/submit`
5. Record saved to Firestore `attendanceRecords` + local JSON
6. Student portal reflects updated attendance % in real time

### 4.3 AI Chat Flow
1. User sends message in `ChatbotScreen`
2. `GroqAiService.getChatCompletion()` sends to Groq Cloud with role-specific system prompt
3. Response sanitized (no asterisks/markdown) and returned
4. Conversation history stored locally (and optionally in Firestore)
5. For RAG: PDF content extracted, embedded, and relevant chunks sent as context

### 4.4 Multi-College Tenant Isolation
1. Every record has `collegeId` field
2. All API queries filter by `collegeId` derived from authenticated user's profile
3. Super Admin can query across all colleges; other roles restricted to their college
4. HOD restricted to their `departmentId` within their college

---

## 5. Security Model

- **Authentication:** Firebase Auth (email/password + Google ID token), email verification required
- **Authorization:** Role-based access control via `require_role()` dependency
- **Tenant Isolation:** `collegeId` + `departmentId` enforced on all data queries
- **Admin Approval:** New accounts created with `status: "PENDING"`, admin must approve to `APPROVED`
- **Privileged Roles:** Faculty/HOD/Principal/College Admin/Super Admin accounts must be provisioned by authorized administrator (cannot self-register)
- **Data Privacy:** Personal document drive is per-user; cross-user access explicitly denied
- **Audit Logs:** All create/update/delete operations logged with actor, role, timestamp, details
- **No secrets in client:** API keys configured server-side only; `.env` never committed to Android project

---

## 6. AI Architecture

- **Primary AI Service:** Groq Cloud (models: qwen/qwen3.8-27b, openai/gpt-oss-120b, etc.)
- **Secondary AI Service:** OpenRouter (fallback)
- **Tertiary:** Gemini (commented out, available for activation)
- **Role-specific system prompts** for Student, Faculty, HOD, Principal, College Admin, Super Admin
- **RAG pipeline:** PDF upload → text extraction → chunking → embedding → semantic search → context injection into LLM
- **AI access enforcement:** Server-side permission check before AI responds with institutional data
- **Usage monitoring:** AI request logs stored for Super Admin monitoring

---

## 7. Responsive Design

- **Window width classes:** COMPACT (<600dp), MEDIUM (600-840dp), EXPANDED (>=840dp)
- **Window height classes:** COMPACT (<480dp), MEDIUM (480-900dp), EXPANDED (>=900dp)
- **Navigation:** Bottom bar on phones, left NavigationRail on tablets/landscape
- **Layout:** Adaptive containers with `widthIn(max=...)`, `BoxWithConstraints` for conditional layouts
- **Target devices:** Android phones, tablets, foldables, desktop/docked (Chrome OS, etc.)

---

## 8. Deployment & Configuration

- **Android:** Build with `./gradlew.bat :app:assembleDebug -PAGENTRA_BACKEND_URL=https://your-backend.example.com`
- **FastAPI:** `cd backend && pip install -r requirements.txt && uvicorn main:app --host 0.0.0.0 --port 8000`
- **Express:** `cd backend && npm install && npm start`
- **Environment variables:** `.env` on backend host only (never in Android project)
- **Firebase:** `google-services.json` in `app/` (never committed to public repo)
- **Firestore rules:** `firestore.rules` must be tested in Firebase Emulator Suite

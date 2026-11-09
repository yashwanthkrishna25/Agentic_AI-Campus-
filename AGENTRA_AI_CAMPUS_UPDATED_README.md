# AGENTRA AI CAMPUS — Complete Multi-College Smart Campus Management System

Native Android application built with Kotlin, Jetpack Compose, Material 3, CameraX, ML Kit, Retrofit, and FastAPI backend with live Groq Cloud AI inference.

---

## 1. Upgraded Android APK Release

The compiled, verified, and signed debug APK is ready for deployment:

* **File Location:** [`app-debug.apk`](file:///D:/Agentra-AI-Campus-main/Agentra-AI-Campus-main/app/build/outputs/apk/debug/app-debug.apk)
* **File Path:** `D:\Agentra-AI-Campus-main\Agentra-AI-Campus-main\app\build\outputs\apk\debug\app-debug.apk`
* **Size:** ~69.3 MB (72,714,210 bytes)
* **Target SDK:** 36 | **Min SDK:** 24 | **Build Status:** PASSED (`BUILD SUCCESSFUL in 8m 52s`)

### Installation via ADB
```powershell
adb install -r "D:\Agentra-AI-Campus-main\Agentra-AI-Campus-main\app\build\outputs\apk\debug\app-debug.apk"
```

---

## 2. Core Implemented Systems

### A. Live Groq Cloud AI Engine
- **Inference Model:** `openai/gpt-oss-120b` running on Groq LPU inference.
- **Zero-Asterisk Output:** Plain-text institutional formatting without any asterisks (`*` or `**`) or markdown formatting artifacts.
- **Role-Aware Contexts:** Specific intelligence co-pilots for Student, Faculty, HOD, Principal, College Admin, and Super Admin.
- **Microservices:**
  - `/api/chat`: Role-contextual campus conversational assistant.
  - `/api/ai/analyze-document`: Institutional syllabus, policy, and paper analysis.
  - `/api/ai/generate-questions`: Bloom's taxonomy examination question generator.
  - `/api/ai/placement-prep`: Placement preparation with technical & HR question generation.

### B. Biometric Face & Eye Recognition Automatic Attendance
- **Hands-Free Marking:** ML Kit face detection continuously tracks facial landmarks and eye openness probabilities (`leftEyeOpenProb >= 0.20`, `rightEyeOpenProb >= 0.20`).
- **Instant Attendance:** When face and eyes are detected and authenticated against enrolled 128-D ArcFace biometric templates, attendance is **automatically marked** without requiring manual button taps.
- **Interactive Viewfinder:** Real-time animated reticles overlay glowing cyan/emerald target markers directly over both eyes.

### C. Six-Role Multi-Tenant RBAC Architecture
- **Tenant Isolation:** Every record is partitioned by `collegeId`.
- **Complete Role Matrix:**
  1. **Student:** Profile, attendance analytics, assignments, examinations, results, hall tickets, certificates, placement training, and AI mentor.
  2. **Faculty:** Class timetable, face & eye attendance marking, assignment evaluation, marks entry, study material distribution, leave requests, and AI lesson planner.
  3. **HOD:** Departmental overview, faculty workload tracking, syllabus progress, elective allocations, student approvals, and department-level AI analytics.
  4. **Principal:** College-wide analytics, institutional performance, faculty leave sign-offs, campus announcements, event approvals, and institutional executive AI.
  5. **College Admin:** College onboarding, departments, academic years, semesters, sections, subjects, faculty-to-subject allocations, and student rosters.
  6. **Super Admin:** Multi-college provisioning, global audit logs, system-wide analytics, tenant suspension/activation, and cross-college security policies.

---

## 3. Backend Execution

```powershell
# Navigate to backend directory and start FastAPI server
cd D:\Agentra-AI-Campus-main\Agentra-AI-Campus-main\backend
python -m uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Running the End-to-End Test Suite
```powershell
python D:\Agentra-AI-Campus-main\Agentra-AI-Campus-main\backend\test_api_endpoints.py
```
*(All 14 test suites pass with 100% success).*


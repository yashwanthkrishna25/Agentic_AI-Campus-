# AGENTRA AI CAMPUS — API Documentation

**Base URL:** `https://your-backend.example.com` (configured via `AGENTRA_BACKEND_URL` Gradle property)
**Auth:** Firebase ID Token sent as `Authorization: Bearer <idToken>`
**Version:** 2.1.0
**Content-Type:** `application/json`

---

## Authentication

All protected endpoints require a valid Firebase ID Token in the `Authorization: Bearer <token>` header. Super Admin bypasses college/department restrictions.

### Header
```
Authorization: Bearer <firebase_id_token>
```

---

## 1. Health

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/health` | GET | None | Service health check |

---

## 2. OTP Verification

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/otp/send` | POST | None | Send 6-digit email verification code |
| `/api/otp/verify` | POST | None | Verify OTP code |

**Request (send):**
```json
{ "email": "student@college.edu" }
```

**Request (verify):**
```json
{ "email": "student@college.edu", "otp": "123456" }
```

---

## 3. College Management (Super Admin / College Admin)

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/colleges` | POST | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN | Create college |
| `/api/colleges` | GET | Any role | List colleges (Super Admin: all; others: own) |
| `/api/colleges/{id}` | GET | College access | Get college details |
| `/api/colleges/{id}` | PUT | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN | Update college |
| `/api/colleges/{id}/suspend` | POST | SUPER_ADMIN, ADMIN | Suspend college |
| `/api/colleges/{id}/activate` | POST | SUPER_ADMIN, ADMIN | Activate college |

**Create Request:**
```json
{ "name": "KSRM College", "code": "KSRMCE", "city": "Kadapa", "adminEmail": "admin@ksrmce.ac.in", "status": "Active" }
```

---

## 4. Department Management

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/departments` | POST | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN, PRINCIPAL | Create department |
| `/api/departments` | GET | College access | List departments |
| `/api/departments/{id}` | GET | College access | Get department |
| `/api/departments/{id}` | PUT | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN, PRINCIPAL | Update department |

**Departments:** CSE, AIML, ECE, EEE, Civil, Mechanical, Other (configurable)

---

## 5. Academic Management (Years, Semesters, Sections, Subjects)

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/academic-years` | POST | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN, PRINCIPAL | Create academic year |
| `/api/academic-years` | GET | College access | List academic years |
| `/api/semesters` | POST | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN, PRINCIPAL | Create semester |
| `/api/semesters` | GET | College access | List semesters |
| `/api/sections` | POST | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN, PRINCIPAL, HOD | Create section |
| `/api/sections` | GET | College access | List sections |
| `/api/subjects` | POST | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN, PRINCIPAL, HOD | Create subject |
| `/api/subjects` | GET | College access | List subjects |

**Years:** 1st–4th **Semesters:** 1–8 **Sections:** Multiple per semester

---

## 6. Faculty & Student Allocations

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/faculty-allocations` | POST | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN, PRINCIPAL, HOD | Allocate faculty to subject/section |
| `/api/faculty-allocations` | GET | College access | List allocations |
| `/api/student-allocations` | POST | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN, PRINCIPAL, HOD | Allocate student to dept/year/sem/section |
| `/api/student-allocations` | GET | College access | List allocations |

---

## 7. Timetable

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/timetable-slots` | POST | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN, PRINCIPAL, HOD | Create timetable slot |
| `/api/timetable-slots` | GET | College access | List timetable slots |

---

## 8. Biometrics (Face Recognition)

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/biometrics/enroll` | POST | FACULTY, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Enroll face template |
| `/api/biometrics/recognize` | POST | FACULTY, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Recognize faces (cosine similarity) |
| `/api/biometrics/delete/{studentId}` | DELETE | ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Delete face template |

---

## 9. Attendance

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/attendance/submit` | POST | FACULTY, HOD, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Submit attendance session |
| `/api/attendance/correct` | POST | FACULTY, HOD, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Correct attendance record |
| `/api/attendance/history` | GET | Any role | Attendance history |
| `/api/attendance/student/{id}` | GET | Any role | Student attendance summary |

---

## 10. Leave Requests

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/leave/apply` | POST | Any role | Apply for leave |
| `/api/leave/pending` | GET | HOD, PRINCIPAL, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | List pending leaves |
| `/api/leave/my` | GET | Any role | My leave applications |
| `/api/leave/decide` | POST | HOD, PRINCIPAL, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Approve/reject leave |

---

## 11. Assignments & Submissions

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/assignments/create` | POST | FACULTY, HOD, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Create assignment |
| `/api/assignments/list` | GET | Any role | List assignments |
| `/api/assignments/{id}` | GET | Any role | Get assignment details |
| `/api/assignments/submit` | POST | STUDENT | Submit assignment (PDF) |
| `/api/assignments/submissions` | GET | Any role | List submissions |
| `/api/assignments/grade` | POST | FACULTY, HOD, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Grade submission |

---

## 12. Announcements

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/announcements/create` | POST | FACULTY, HOD, PRINCIPAL, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Create announcement |
| `/api/announcements/list` | GET | Any role | List announcements |

---

## 13. Documents (Private Drive + Shared Storage)

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/documents` | POST | Any role | Upload document |
| `/api/documents` | GET | Any role | List documents (privacy-enforced) |
| `/api/documents/{id}` | GET | Any role | Get document |
| `/api/documents/{id}` | DELETE | Owner / Super Admin | Delete document |
| `/api/document-folders` | POST | Any role | Create folder |
| `/api/document-folders` | GET | Any role | List folders |

**Privacy:** Private docs accessible only to owner + explicit shares. Shared-with-college docs visible to all in college.

---

## 14. Messages

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/messages` | POST | Any role | Send message |
| `/api/messages` | GET | Any role | List messages |
| `/api/messages/{id}/read` | POST | Receiver / Super Admin | Mark message read |

---

## 15. Tasks (To-Do)

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/tasks` | POST | Any role | Create task |
| `/api/tasks` | GET | Any role | List tasks (own) |
| `/api/tasks/{id}` | PUT | Owner / Super Admin | Update task status |

---

## 16. Exam Results

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/exam-results` | POST | FACULTY, HOD, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN, PRINCIPAL | Create exam result |
| `/api/exam-results` | GET | Any role | List results (students see only published) |
| `/api/exam-results/{id}/publish` | POST | HOD, PRINCIPAL, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Publish result |

---

## 17. Certificates & Hall Tickets

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/certificates` | POST | HOD, PRINCIPAL, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Create certificate |
| `/api/certificates` | GET | Any role | List certificates |
| `/api/hall-tickets` | POST | HOD, PRINCIPAL, ADMIN, COLLEGE_ADMIN, SUPER_ADMIN | Create hall ticket |
| `/api/hall-tickets` | GET | Any role | List hall tickets |

---

## 18. AI Assistant

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/chat` | POST | Any role | AI chat (role-aware, Groq/OpenRouter) |

---

## 19. Analytics

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/analytics/dashboard` | GET | College access | College dashboard analytics |
| `/api/analytics/platform` | GET | SUPER_ADMIN | Platform-wide analytics |

---

## 20. Audit Logs

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/audit-logs` | GET | SUPER_ADMIN, ADMIN, COLLEGE_ADMIN | Audit log entries |

---

## 21. User Management

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/api/users` | GET | College access | List users |
| `/api/users/{uid}` | GET | College access | Get user |
| `/api/users/{uid}/approve` | PUT | ADMIN, COLLEGE_ADMIN, SUPER_ADMIN, PRINCIPAL | Approve user |
| `/api/users/{uid}/suspend` | PUT | ADMIN, COLLEGE_ADMIN, SUPER_ADMIN, PRINCIPAL | Suspend user |

---

## Response Format

**Success:**
```json
{ "success": true, "data": { ... }, "message": "Optional message" }
```

**Error:**
```json
{ "success": false, "error": "Error description" }
```

**HTTP Status Codes:** 200 (success), 400 (bad request), 401 (unauthorized), 403 (forbidden), 404 (not found), 500 (server error)

---

## Role Abbreviations

| Role | Abbreviation |
|------|-------------|
| Super Admin | SUPER_ADMIN |
| College Admin | ADMIN / COLLEGE_ADMIN |
| Principal | PRINCIPAL |
| HOD | HOD |
| Faculty | FACULTY |
| Student | STUDENT |

---

## Tenant Isolation Rules

- Every record includes `collegeId`
- Super Admin: cross-college access
- College Admin / Principal: own college only
- HOD / Faculty: own department within own college
- Student: own records only within own college
- Document privacy: owner-only unless explicitly shared

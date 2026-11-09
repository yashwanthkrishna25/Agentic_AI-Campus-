# AGENTRA AI CAMPUS — Role Permission Matrix

**Version:** 1.0
**Date:** 01 Nov 2026
**Roles:** Student, Faculty, HOD, Principal, College Admin, Super Admin

## Legend
- ✅ = Full Access (Create, Read, Update, Delete)
- 🔍 = Read Only
- ⚠ = Limited / Restricted (own records or own department)
- ❌ = No Access

---

## 1. Authentication & Account

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| Self-register (Student only) | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| Authorized provisioning (staff) | ❌ | ✅* | ✅* | ✅* | ✅* | ✅* |
| Login | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Password reset | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Logout | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Profile view/edit (own) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Face enrollment | ✅ | ⚠ (faculty login) | ✅ | ✅ | ✅ | ✅ |

*Staff accounts must be provisioned by an authorized administrator; cannot self-register.

---

## 2. College Management

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| Create college | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| Edit college | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |
| Activate / Suspend college | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| View own college | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| View all colleges | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 3. Department Management

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| Create department | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Edit department | ❌ | ❌ | ✅ (own) | ✅ | ✅ | ✅ |
| View departments | 🔍 | 🔍 | ✅ (own) | ✅ (own college) | ✅ (own college) | ✅ (all) |
| Assign HOD | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |

---

## 4. Academic Management (Years, Semesters, Sections, Subjects)

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| Create academic year | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Create semester | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Create section | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Create subject | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| View timetable | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Configure timetable | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |

---

## 5. Student Module

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| View own profile | ✅ | ❌ | ⚠ | ✅ | ✅ | ✅ |
| View subject-wise attendance | ✅ | ✅ (own class) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Face recognition attendance | ⚠ (kiosk scan) | ✅ | ✅ | ✅ | ✅ | ✅ |
| View syllabus | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| View timetable | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Access course materials | ✅ | ✅ (own uploads) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Upload course materials | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| View assignments | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Submit assignments | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| View quiz | ✅ | ✅ (own) | ✅ | ✅ | ✅ | ✅ |
| View marks (Mid-1, Mid-2, Semester) | ✅ (own) | ✅ (own class) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Enter marks | ❌ | ✅ (own subjects) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Publish results | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| View SGPA/CGPA | ✅ (own) | ✅ (own class) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Personal document drive | ✅ (own) | ❌ | ❌ | ❌ | ❌ | ❌ |
| View digital certificates | ✅ (own) | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| View hall tickets | ✅ (own) | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| AI learning assistant | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| PDF question-answering (RAG) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Placement preparation | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Resume assistance | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Notifications (own) | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Daily tasks / to-do | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 6. Faculty Module

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| Faculty profile | ❌ | ✅ (own) | ✅ | ✅ | ✅ | ✅ |
| Teaching allocations | ❌ | ✅ (own) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Take attendance (face/manual) | ❌ | ✅ (own subjects) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Upload study materials | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| Create assignments | ❌ | ✅ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Grade submissions | ❌ | ✅ (own) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Enter Mid-1/Mid-2 marks | ❌ | ✅ (own subjects) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Create quizzes / question bank | ❌ | ✅ | ✅ (own dept) | ✅ | ✅ | ✅ |
| AI lesson planning / question gen | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Class performance analytics | ❌ | ✅ (own class) | ✅ (own dept) | ✅ | ✅ | ✅ |
| Faculty-student messaging | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| HOD communication | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Faculty workload analytics | ❌ | 🔍 (own) | ✅ (own dept) | ✅ | ✅ | ✅ |

---

## 7. HOD Module (Restricted to assigned department)

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| Department dashboard | ❌ | ❌ | ✅ (own) | ✅ | ✅ | ✅ |
| Department faculty management | ❌ | ❌ | ✅ (own) | ✅ | ✅ | ✅ |
| Faculty assignments | ❌ | ❌ | ✅ (own) | ✅ | ✅ | ✅ |
| Subject allocation | ❌ | ❌ | ✅ (own) | ✅ | ✅ | ✅ |
| Academic timetable | ❌ | ❌ | ✅ (own) | ✅ | ✅ | ✅ |
| Student management | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Attendance monitoring | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Assignment monitoring | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Marks monitoring | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Result analysis | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Faculty leave approvals | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Department announcements | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Remedial classes | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| Reports & exports | ❌ | ❌ | ✅ (own dept) | ✅ | ✅ | ✅ |
| HOD AI assistant | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |

---

## 8. Principal Module (Own college only)

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| College-wide dashboard | ❌ | ❌ | ❌ | ✅ (own) | ✅ (own) | ✅ (all) |
| Department drill-down | ❌ | ❌ | ⚠ | ✅ (own) | ✅ (own) | ✅ (all) |
| Academic performance reports | ❌ | ❌ | ⚠ | ✅ (own) | ✅ (own) | ✅ (all) |
| Attendance reports | ❌ | ❌ | ⚠ | ✅ (own) | ✅ (own) | ✅ (all) |
| Examination reports | ❌ | ❌ | ⚠ | ✅ (own) | ✅ (own) | ✅ (all) |
| Placement reports | ❌ | ❌ | ⚠ | ✅ (own) | ✅ (own) | ✅ (all) |
| Approval workflows | ❌ | ❌ | ⚠ | ✅ (own) | ✅ (own) | ✅ (all) |
| College announcements | ❌ | ❌ | ❌ | ✅ (own) | ✅ (own) | ✅ (all) |
| AI executive assistant | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| Graphs & visualizations | ❌ | ❌ | ⚠ | ✅ (own) | ✅ (own) | ✅ (all) |

---

## 9. College Admin Module (Own college only)

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| College configuration | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| Department management | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| Principal account management | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| HOD management | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| Faculty management | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| Student management | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| Bulk Excel/CSV import | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| Timetable / Exam / Attendance config | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| Certificate / Hall ticket management | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| User access management | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| Storage management | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| College reports | ❌ | ❌ | ❌ | ❌ | ✅ (own) | ✅ (all) |
| AI administrative assistant | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ |

---

## 10. Super Admin Module (All colleges)

| Feature | Student | Faculty | HOD | Principal | College Admin | Super Admin |
|---------|---------|---------|-----|-----------|---------------|-------------|
| Multi-college dashboard | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| College onboarding / creation | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| College activation / suspension | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Principal / HOD / Faculty / Student management | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Platform-wide analytics | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| System monitoring | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| AI usage monitoring | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Audit logs | ❌ | ❌ | ❌ | ⚠ | ⚠ | ✅ |
| Backup management | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Platform configuration | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Role & permission management | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Global notifications | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |
| Super Admin AI assistant | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

---

## 11. Document Privacy Matrix

| Document Type | Owner Access | Same Dept | Same College | Other College |
|---------------|-------------|-----------|--------------|---------------|
| Personal private drive | ✅ | ❌ | ❌ | ❌ |
| Explicitly shared (user) | ✅ | ✅ (if shared) | ✅ (if shared) | ❌ |
| Department shared | ✅ | ✅ | ❌ | ❌ |
| College shared | ✅ | ✅ | ✅ | ❌ |

---

## 12. AI Access Restrictions

AI responses are scoped to the authenticated user's role and college. AI never exposes:
- Private documents of other users
- Attendance/marks of students outside the user's authorized scope
- Cross-college data (unless Super Admin)
- Audit logs (unless authorized role)

All AI usage is logged in `aiUsageLogs` for Super Admin monitoring.

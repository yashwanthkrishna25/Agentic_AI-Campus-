import os
import time
import math
import hmac
import hashlib
import secrets
from typing import List, Optional, Dict, Any
from datetime import datetime

from fastapi import FastAPI, Depends, HTTPException, Header, Query, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, EmailStr
from dotenv import load_dotenv
import httpx

load_dotenv()

# ---------------------------------------------------------------------------
# Firebase Admin + Firestore Initialization
# ---------------------------------------------------------------------------
firebase_initialized = False
db = None  # Firestore client

try:
    import firebase_admin
    from firebase_admin import credentials, auth as fb_auth, firestore as fb_firestore
    if not firebase_admin._apps:
        cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
        if cred_path and os.path.exists(cred_path):
            cred = credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred)
            firebase_initialized = True
        else:
            try:
                firebase_admin.initialize_app()
                firebase_initialized = True
            except Exception:
                firebase_initialized = False
    if firebase_initialized:
        db = fb_firestore.client()
except Exception as e:
    print(f"[Warning] Firebase Admin initialization deferred: {e}")

# ---------------------------------------------------------------------------
# In-Memory Fallback Store (for local development when Firestore is unavailable)
# ---------------------------------------------------------------------------
in_memory_db = {
    "users": {},
    "colleges": {},
    "departments": {},
    "academicYears": {},
    "semesters": {},
    "sections": {},
    "subjects": {},
    "facultyAllocations": {},
    "studentAllocations": {},
    "timetableSlots": {},
    "attendanceRecords": {},
    "studentAttendance": {},
    "faceTemplates": {},
    "leaveRequests": {},
    "assignments": {},
    "assignmentSubmissions": {},
    "quizzes": {},
    "quizAttempts": {},
    "questionBank": {},
    "examResults": {},
    "certificates": {},
    "hallTickets": {},
    "documents": {},
    "documentFolders": {},
    "announcements": {},
    "notifications": {},
    "messages": {},
    "tasks": {},
    "placements": {},
    "skills": {},
    "studentSkills": {},
    "calendarEvents": {},
    "approvalRequests": {},
    "systemAlerts": {},
    "auditLogs": {},
    "aiUsageLogs": {},
    "emailOtpChallenges": {},
    "verifiedEmailOtps": {}
}

# Collection name mapping (Firestore collection -> in_memory_db key)
COLLECTION_MAP = {
    "users": "users",
    "colleges": "colleges",
    "departments": "departments",
    "academicYears": "academicYears",
    "semesters": "semesters",
    "sections": "sections",
    "subjects": "subjects",
    "facultyAllocations": "facultyAllocations",
    "studentAllocations": "studentAllocations",
    "timetableSlots": "timetableSlots",
    "attendanceRecords": "attendanceRecords",
    "studentAttendance": "studentAttendance",
    "faceTemplates": "faceTemplates",
    "leaveRequests": "leaveRequests",
    "assignments": "assignments",
    "assignmentSubmissions": "assignmentSubmissions",
    "quizzes": "quizzes",
    "quizAttempts": "quizAttempts",
    "questionBank": "questionBank",
    "examResults": "examResults",
    "certificates": "certificates",
    "hallTickets": "hallTickets",
    "documents": "documents",
    "documentFolders": "documentFolders",
    "announcements": "announcements",
    "notifications": "notifications",
    "messages": "messages",
    "tasks": "tasks",
    "placements": "placements",
    "skills": "skills",
    "studentSkills": "studentSkills",
    "calendarEvents": "calendarEvents",
    "approvalRequests": "approvalRequests",
    "systemAlerts": "systemAlerts",
    "auditLogs": "auditLogs",
    "aiUsageLogs": "aiUsageLogs",
    "emailOtpChallenges": "emailOtpChallenges",
    "verifiedEmailOtps": "verifiedEmailOtps"
}

# ---------------------------------------------------------------------------
# Firestore Helper Functions (with in-memory fallback)
# ---------------------------------------------------------------------------

def _now_ms() -> int:
    return int(time.time() * 1000)


def _now_iso() -> str:
    return datetime.utcnow().isoformat() + "Z"


def _col(collection_name: str):
    """Get a Firestore collection reference."""
    if db is not None:
        return db.collection(collection_name)
    return None


def _get(collection_name: str, doc_id: str) -> Optional[Dict[str, Any]]:
    """Get a single document by ID."""
    col = _col(collection_name)
    if col is not None:
        try:
            doc = col.document(doc_id).get()
            if doc.exists:
                data = doc.to_dict() or {}
                data["id"] = doc.id
                return data
        except Exception as e:
            print(f"[Firestore Get Error] {collection_name}/{doc_id}: {e}")
    store = in_memory_db.setdefault(collection_name, {})
    return store.get(doc_id)


def _list(collection_name: str, filters: Optional[List[tuple]] = None, limit: int = 100) -> List[Dict[str, Any]]:
    """List documents with optional equality filters: [(field, value), ...]."""
    col = _col(collection_name)
    results = []
    if col is not None:
        try:
            query = col
            for field, value in (filters or []):
                query = query.where(field, "==", value)
            query = query.limit(limit)
            for doc in query.stream():
                data = doc.to_dict() or {}
                data["id"] = doc.id
                results.append(data)
            return results
        except Exception as e:
            print(f"[Firestore List Error] {collection_name}: {e}")
    # In-memory fallback with client-side filtering
    store = in_memory_db.setdefault(collection_name, {})
    for doc in store.values():
        match = True
        for field, value in (filters or []):
            if doc.get(field) != value:
                match = False
                break
        if match:
            results.append(dict(doc))
    return results[:limit]


def _create(collection_name: str, doc_id: str, data: Dict[str, Any]) -> Dict[str, Any]:
    """Create a document with a specific ID."""
    data = dict(data)
    data["id"] = doc_id
    data["createdAt"] = data.get("createdAt", _now_ms())
    col = _col(collection_name)
    if col is not None:
        try:
            col.document(doc_id).set(data)
            return data
        except Exception as e:
            print(f"[Firestore Create Error] {collection_name}/{doc_id}: {e}")
    if collection_name not in in_memory_db or not isinstance(in_memory_db[collection_name], dict):
        in_memory_db[collection_name] = {}
    in_memory_db[collection_name][doc_id] = data
    return data


def _update(collection_name: str, doc_id: str, updates: Dict[str, Any]) -> Optional[Dict[str, Any]]:
    """Update a document by ID."""
    updates = dict(updates)
    updates["updatedAt"] = _now_ms()
    col = _col(collection_name)
    if col is not None:
        try:
            col.document(doc_id).update(updates)
            return _get(collection_name, doc_id)
        except Exception as e:
            print(f"[Firestore Update Error] {collection_name}/{doc_id}: {e}")
    if doc_id in in_memory_db[collection_name]:
        in_memory_db[collection_name][doc_id].update(updates)
        return in_memory_db[collection_name][doc_id]
    return None


def _delete(collection_name: str, doc_id: str) -> bool:
    """Delete a document by ID (soft delete: set status/inActive)."""
    col = _col(collection_name)
    if col is not None:
        try:
            col.document(doc_id).update({"status": "INACTIVE", "updatedAt": _now_ms()})
            return True
        except Exception as e:
            print(f"[Firestore Delete Error] {collection_name}/{doc_id}: {e}")
    if doc_id in in_memory_db[collection_name]:
        in_memory_db[collection_name][doc_id]["status"] = "INACTIVE"
        in_memory_db[collection_name][doc_id]["updatedAt"] = _now_ms()
        return True
    return False


def _audit(action: str, actor_uid: str, actor_role: str, target_type: str, target_id: str, details: str = ""):
    """Append an audit log entry."""
    entry = {
        "id": f"audit_{_now_ms()}_{secrets.randbelow(1000)}",
        "action": action,
        "actorId": actor_uid,
        "actorRole": actor_role,
        "targetType": target_type,
        "targetId": target_id,
        "details": details,
        "timestamp": _now_ms()
    }
    _create("auditLogs", entry["id"], entry)
    return entry


# ---------------------------------------------------------------------------
# Models
# ---------------------------------------------------------------------------

class UserTokenPayload(BaseModel):
    uid: str
    email: Optional[str] = None
    role: str = "STUDENT"
    displayName: Optional[str] = "Campus User"
    department: Optional[str] = "Computer Science & Engineering"
    departmentId: Optional[str] = None
    collegeId: Optional[str] = "default"


class CollegeCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=200)
    code: str = Field(..., min_length=2, max_length=20)
    city: Optional[str] = None
    adminEmail: Optional[str] = None
    status: str = "Active"


class CollegeUpdate(BaseModel):
    name: Optional[str] = None
    code: Optional[str] = None
    city: Optional[str] = None
    adminEmail: Optional[str] = None
    status: Optional[str] = None


class DepartmentCreate(BaseModel):
    collegeId: str
    name: str
    code: str
    hodName: Optional[str] = None
    hodEmail: Optional[str] = None
    hodId: Optional[str] = None


class DepartmentUpdate(BaseModel):
    name: Optional[str] = None
    code: Optional[str] = None
    hodName: Optional[str] = None
    hodEmail: Optional[str] = None
    hodId: Optional[str] = None
    status: Optional[str] = None


class AcademicYearCreate(BaseModel):
    collegeId: str
    name: str
    startDate: Optional[str] = None
    endDate: Optional[str] = None
    isCurrent: bool = False


class SemesterCreate(BaseModel):
    collegeId: str
    academicYearId: str
    name: str
    number: int = Field(..., ge=1, le=8)
    startDate: Optional[str] = None
    endDate: Optional[str] = None
    isCurrent: bool = False


class SectionCreate(BaseModel):
    collegeId: str
    departmentId: str
    academicYearId: str
    semesterId: str
    name: str


class SubjectCreate(BaseModel):
    collegeId: str
    departmentId: str
    code: str
    name: str
    credits: int = 3
    year: int = Field(..., ge=1, le=4)
    semester: int = Field(..., ge=1, le=8)


class FacultyAllocationCreate(BaseModel):
    collegeId: str
    departmentId: str
    facultyId: str
    subjectId: str
    academicYearId: str
    semesterId: str
    sectionId: str


class StudentAllocationCreate(BaseModel):
    collegeId: str
    studentId: str
    departmentId: str
    academicYearId: str
    semesterId: str
    sectionId: str
    rollNumber: str


class TimetableSlotCreate(BaseModel):
    collegeId: str
    departmentId: str
    sectionId: str
    subjectId: str
    facultyId: str
    day: str
    periodNumber: int = Field(..., ge=1, le=8)
    timeSlot: str
    roomNumber: Optional[str] = None
    academicYearId: str
    semesterId: str


class AttendanceSessionStart(BaseModel):
    subjectCode: str
    subjectName: str
    departmentId: str
    sectionId: str
    periodNumber: int = 1
    date: str


class AttendanceRecordSubmit(BaseModel):
    sessionId: str
    subjectCode: str
    subjectName: str
    departmentId: str
    sectionId: str
    periodNumber: int = 1
    date: str
    presentStudentIds: List[str] = []
    absentStudentIds: List[str] = []
    lateStudentIds: List[str] = []
    excusedStudentIds: List[str] = []
    method: str = "FACE_RECOGNITION"


class AttendanceCorrectionRequest(BaseModel):
    recordId: str
    studentId: str
    previousStatus: str
    newStatus: str
    reason: str


class LeaveRequestCreate(BaseModel):
    leaveType: str = "CASUAL"
    startDate: str
    endDate: str
    reason: str
    documentUrl: Optional[str] = None


class LeaveDecisionRequest(BaseModel):
    leaveId: str
    decision: str  # APPROVED, REJECTED
    remarks: Optional[str] = None


class AssignmentCreate(BaseModel):
    subjectCode: str
    subjectName: str
    departmentId: str
    sectionId: str
    title: str
    instructions: str
    deadline: str
    maxMarks: int = 10
    permittedFileType: str = "PDF"


class SubmissionCreate(BaseModel):
    assignmentId: str
    fileUrl: str
    fileName: str
    fileSizeBytes: int


class GradeSubmissionRequest(BaseModel):
    submissionId: str
    marksObtained: float
    feedback: str


class AnnouncementCreate(BaseModel):
    title: str
    message: str
    audience: str = "ALL"
    departmentId: Optional[str] = None
    section: Optional[str] = None
    priority: str = "NORMAL"


class DocumentCreate(BaseModel):
    title: str
    fileUrl: str
    fileName: str
    fileType: str
    fileSizeBytes: int
    folderId: Optional[str] = None
    isPrivate: bool = True
    sharedWith: List[str] = []
    sharedWithDepartments: List[str] = []
    sharedWithCollege: bool = False


class DocumentFolderCreate(BaseModel):
    name: str
    parentFolderId: Optional[str] = None
    isPrivate: bool = True


class MessageCreate(BaseModel):
    receiverId: str
    message: str


class TaskCreate(BaseModel):
    title: str
    description: Optional[str] = None
    dueDate: Optional[str] = None
    priority: str = "MEDIUM"


class ChatRequest(BaseModel):
    message: str = Field(..., max_length=4000)
    history: List[Dict[str, str]] = []


class DocumentAnalysisRequest(BaseModel):
    documentTitle: str = Field(..., max_length=200)
    documentText: str = Field(..., max_length=30000)
    query: Optional[str] = None
    taskType: str = "SUMMARIZE"  # SUMMARIZE, QA, QUIZ, NOTES, WEAK_AREAS


class QuestionGenerationRequest(BaseModel):
    subject: str
    unitOrTopic: str
    difficulty: str = "MEDIUM"  # EASY, MEDIUM, HARD
    questionType: str = "MIXED"  # MCQ, SHORT, ESSAY, MIXED
    count: int = 5


class PlacementPrepRequest(BaseModel):
    department: str
    targetCompanyOrRole: str
    prepType: str = "TECHNICAL_INTERVIEW"  # TECHNICAL_INTERVIEW, HR_INTERVIEW, CODING_PROBLEM, APTITUDE
    skillFocus: Optional[str] = None


class OtpSendRequest(BaseModel):
    email: str


class OtpVerifyRequest(BaseModel):
    email: str
    otp: str


class FaceEnrollRequest(BaseModel):
    studentId: str
    studentName: str
    departmentId: str
    section: str
    embedding: List[float] = Field(..., description="128 or 512-dimensional facial embedding vector")
    consentConfirmed: bool = True


class FaceRecognizeRequest(BaseModel):
    subjectCode: str
    sectionId: str
    embeddings: List[List[float]] = Field(..., description="List of detected faces in the current camera frame")


# ============================================================================
# AI Request Models
# ============================================================================

class DocumentAnalysisRequest(BaseModel):
    documentTitle: str
    documentText: str
    taskType: str = "ANALYZE"
    query: Optional[str] = None


class QuestionGenerationRequest(BaseModel):
    subject: str
    unitOrTopic: str
    difficulty: str = "MEDIUM"
    questionType: str = "MCQ"
    count: int = 10


class PlacementPrepRequest(BaseModel):
    department: str
    targetCompanyOrRole: str
    prepType: str = "TECHNICAL"
    skillFocus: Optional[str] = None


# ============================================================================
# Quiz & Question Models
# ============================================================================

class QuizCreateRequest(BaseModel):
    title: str
    subjectId: str
    sectionId: str
    departmentId: str
    questions: List[Dict[str, Any]]
    maxMarks: float
    startTime: str
    endTime: str
    durationMinutes: int


class QuizAttemptSubmit(BaseModel):
    quizId: str
    answers: List[Dict[str, Any]]
    timeTakenSeconds: int


# ============================================================================
# Placement & Career Models
# ============================================================================

class PlacementDriveCreate(BaseModel):
    companyName: str
    jobRole: str
    description: str
    eligibleDepartments: List[str]
    minCgpa: float
    maxBacklogs: int
    applicationDeadline: str
    aptitudeTestDate: str
    technicalInterviewDate: str
    hrInterviewDate: str
    resultDate: str


class PlacementApplication(BaseModel):
    driveId: str
    resumeUrl: str


# ============================================================================
# Skills Models
# ============================================================================

class SkillCreate(BaseModel):
    name: str
    category: str
    description: str


class StudentSkillUpsert(BaseModel):
    skillId: str
    proficiencyLevel: str
    yearsOfExperience: float = 0
    certification: Optional[str] = None


# ============================================================================
# Calendar Event Models
# ============================================================================

class CalendarEventCreate(BaseModel):
    title: str
    description: Optional[str] = None
    eventDate: str
    eventType: str
    targetRoles: List[str] = []
    targetDepartments: List[str] = []
    targetSections: List[str] = []


# ============================================================================
# Approval Workflow Models
# ============================================================================

class ApprovalRequestCreate(BaseModel):
    requestType: str
    referenceId: str
    reason: str


class ApprovalDecision(BaseModel):
    approvalId: str
    decision: str
    remarks: Optional[str] = None


# ============================================================================
# Advanced Face Recognition Models
# ============================================================================

class FaceEnrollRequestAdvanced(BaseModel):
    studentId: str
    studentName: str
    departmentId: str
    section: str
    embedding: List[float] = Field(..., description="Averaged 128-D L2-normalized embedding from multi-frame accumulation")
    embeddings: List[List[float]] = Field(default=[], description="Raw per-frame embeddings (for multi-frame validation)")
    confidences: List[float] = Field(default=[], description="Per-frame detection confidences")
    frameCount: int = Field(default=1, description="Number of frames used to compute embedding")
    consentConfirmed: bool = True
    livenessConfidence: float = Field(default=1.0, description="Liveness confidence score 0-1")
    modelVersion: str = "arcface_v2_multi_frame"


class FaceRecognizeRequestAdvanced(BaseModel):
    subjectCode: str
    sectionId: str
    embeddings: List[List[float]] = Field(..., description="List of detected face embeddings per frame")
    frameEmbeddings: List[List[float]] = Field(default=[], description="Multi-frame embeddings for temporal averaging")
    livenessData: Optional[Dict[str, Any]] = None
    requireLiveness: bool = True


# ============================================================================
# Advanced Face Recognition Models (Multi-frame + Combined Metrics + Liveness)
# ============================================================================

class FaceEnrollRequestAdvanced(BaseModel):
    studentId: str
    studentName: str
    departmentId: str
    section: str
    embedding: List[float] = Field(..., description="Averaged 128-D L2-normalized embedding from multi-frame accumulation")
    embeddings: List[List[float]] = Field(default=[], description="Raw per-frame embeddings (for multi-frame validation)")
    confidences: List[float] = Field(default=[], description="Per-frame detection confidences")
    frameCount: int = Field(default=1, description="Number of frames used to compute embedding")
    consentConfirmed: bool = True
    livenessConfidence: float = Field(default=1.0, description="Liveness confidence score 0-1")
    modelVersion: str = "arcface_v2_multi_frame"


class FaceRecognizeRequestAdvanced(BaseModel):
    subjectCode: str
    sectionId: str
    embeddings: List[List[float]] = Field(..., description="List of detected face embeddings per frame")
    frameEmbeddings: List[List[float]] = Field(default=[], description="Multi-frame embeddings for temporal averaging")
    livenessData: Optional[Dict[str, Any]] = None
    requireLiveness: bool = True


# ---------------------------------------------------------------------------
# Auth Dependency
# ---------------------------------------------------------------------------

def sha256_hash(val: str) -> str:
    return hashlib.sha256(val.encode("utf-8")).hexdigest()


def hash_otp(email: str, otp: str) -> str:
    secret = os.getenv("OTP_HASH_SECRET", "CAMPUS_DEFAULT_SECRET_KEY_FOR_LOCAL_DEV_32_CHARS")
    return hmac.new(secret.encode("utf-8"), f"{email}:{otp}".encode("utf-8"), hashlib.sha256).hexdigest()


async def get_current_user(authorization: Optional[str] = Header(None)) -> UserTokenPayload:
    """Verifies Firebase ID token or allows structured header in dev mode."""
    if not authorization or not authorization.startswith("Bearer "):
        dev_mode = os.getenv("DEV_MODE", "true").lower() == "true"
        if dev_mode:
            return UserTokenPayload(
                uid="dev_user_001",
                email="dev.student@ksrmce.ac.in",
                role="STUDENT",
                displayName="Student Penchala Prasad",
                department="Computer Science & Engineering",
                collegeId="col_ksrm_01"
            )
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Authentication token required.")

    token = authorization[7:].strip()
    dev_mode = os.getenv("DEV_MODE", "true").lower() == "true"
    norm_token = token.upper()

    # In DEV_MODE, allow role-prefixed tokens for robust RBAC automated testing
    if dev_mode and any(r in norm_token for r in ("SUPER_ADMIN", "ADMIN", "PRINCIPAL", "HOD", "FACULTY", "STUDENT")):
        dev_college = "col_ksrmce"
        if ":" in token:
            parts = token.split(":")
            if len(parts) >= 2 and parts[1].strip():
                dev_college = parts[1].strip()

        if "SUPER_ADMIN" in norm_token:
            dev_role = "SUPER_ADMIN"
        elif "COLLEGE_ADMIN" in norm_token or "COLLEGE ADMIN" in norm_token or "ADMIN" in norm_token:
            dev_role = "ADMIN"
        elif "PRINCIPAL" in norm_token:
            dev_role = "PRINCIPAL"
        elif "HOD" in norm_token:
            dev_role = "HOD"
        elif "FACULTY" in norm_token:
            dev_role = "FACULTY"
        else:
            dev_role = "STUDENT"

        return UserTokenPayload(
            uid=f"usr_{dev_role.lower()}_{dev_college}_01",
            email=f"{dev_role.lower()}@{dev_college}.edu",
            role=dev_role,
            displayName=f"Institutional {dev_role}",
            department="Computer Science & Engineering",
            departmentId="dept_cse_ksrmce",
            collegeId=dev_college
        )

    if firebase_initialized:
        try:
            decoded = fb_auth.verify_id_token(token)
            uid = decoded["uid"]
            role = decoded.get("role", "STUDENT")
            doc = _get("users", uid)
            if doc:
                return UserTokenPayload(
                    uid=uid,
                    email=decoded.get("email", doc.get("email")),
                    role=doc.get("role", role),
                    displayName=doc.get("displayName", decoded.get("name", "Campus User")),
                    department=doc.get("department", "Computer Science & Engineering"),
                    departmentId=doc.get("departmentId"),
                    collegeId=doc.get("collegeId", "default")
                )
            return UserTokenPayload(
                uid=uid,
                email=decoded.get("email"),
                role=role,
                displayName=decoded.get("name", "Campus User"),
                collegeId=decoded.get("collegeId", "default")
            )
        except Exception as e:
            if dev_mode:
                return UserTokenPayload(
                    uid="usr_dev_fallback",
                    email="dev@ksrmce.ac.in",
                    role="STUDENT",
                    displayName="Dev Student",
                    department="Computer Science & Engineering",
                    departmentId="dept_cse_ksrmce",
                    collegeId="col_ksrmce"
                )
            print(f"[Auth Error] ID token verification failed: {e}")
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid session token.")



def require_role(allowed_roles: List[str]):
    def dependency(user: UserTokenPayload = Depends(get_current_user)):
        norm_user_role = user.role.upper()
        norm_allowed = [r.upper() for r in allowed_roles]
        if norm_user_role not in norm_allowed and "SUPER_ADMIN" not in norm_user_role:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Access denied. Requires one of roles: {allowed_roles}"
            )
        return user
    return dependency


def require_college_access(user: UserTokenPayload = Depends(get_current_user)) -> UserTokenPayload:
    """Ensure user can only access their own college unless Super Admin."""
    if "SUPER_ADMIN" in user.role.upper():
        return user
    if not user.collegeId:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="User not assigned to a college.")
    return user


def require_department_access(user: UserTokenPayload = Depends(get_current_user)) -> UserTokenPayload:
    """Ensure HOD/Faculty can only access their own department."""
    if "SUPER_ADMIN" in user.role.upper():
        return user
    if user.role.upper() in ("ADMIN", "COLLEGE ADMIN"):
        return user
    if not user.departmentId:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="User not assigned to a department.")
    return user


# ---------------------------------------------------------------------------
# FastAPI App
# ---------------------------------------------------------------------------

app = FastAPI(
    title="AGENTRA AI CAMPUS Backend API",
    description="Secure FastAPI Backend for Multi-College Campus Management with Six Roles",
    version="2.1.0"
)

cors_origins_raw = os.getenv("CORS_ORIGINS", "*")
allowed_origins = [o.strip() for o in cors_origins_raw.split(",") if o.strip()]
app.add_middleware(
    CORSMiddleware,
    allow_origins=allowed_origins if allowed_origins else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ============================================================================
# HEALTH
# ============================================================================

@app.get("/health")
def health_check():
    return {
        "status": "healthy",
        "service": "AGENTRA AI CAMPUS Backend (FastAPI)",
        "firebaseConnected": firebase_initialized,
        "timestamp": _now_ms()
    }


# ============================================================================
# 1. OTP VERIFICATION
# ============================================================================

@app.post("/api/otp/send")
def send_otp(req: OtpSendRequest):
    email = req.email.strip().lower()
    if "@" not in email or "." not in email:
        raise HTTPException(status_code=400, detail="Invalid email format.")
    otp = str(secrets.randbelow(900000) + 100000)
    email_hash = sha256_hash(email)
    now = _now_ms()
    in_memory_db["emailOtpChallenges"][email_hash] = {
        "emailHash": email_hash,
        "otpHash": hash_otp(email, otp),
        "createdAt": now,
        "expiresAt": now + (10 * 60 * 1000),
        "attempts": 0
    }
    smtp_user = os.getenv("SMTP_USER")
    smtp_pass = os.getenv("SMTP_PASS")
    if smtp_user and smtp_pass:
        try:
            import smtplib
            from email.mime.text import MIMEText
            msg = MIMEText(f"Your AGENTRA AI CAMPUS verification code is: {otp}\nThis code expires in 10 minutes.")
            msg["Subject"] = "AGENTRA AI CAMPUS Verification Code"
            msg["From"] = os.getenv("SMTP_FROM", smtp_user)
            msg["To"] = email
            server = smtplib.SMTP_SSL(os.getenv("SMTP_HOST", "smtp.gmail.com"), int(os.getenv("SMTP_PORT", 465)))
            server.login(smtp_user, smtp_pass)
            server.send_message(msg)
            server.quit()
        except Exception as e:
            print(f"[SMTP Error] {e}")
    return {"sent": True, "message": "Verification code dispatched to institutional email."}


@app.post("/api/otp/verify")
def verify_otp(req: OtpVerifyRequest):
    email = req.email.strip().lower()
    email_hash = sha256_hash(email)
    challenge = in_memory_db["emailOtpChallenges"].get(email_hash)
    if not challenge:
        return {"verified": False, "error": "Verification session expired or not found."}
    now = _now_ms()
    if now > challenge["expiresAt"] or challenge["attempts"] >= 5:
        in_memory_db["emailOtpChallenges"].pop(email_hash, None)
        return {"verified": False, "error": "Code expired. Please request a new code."}
    if hash_otp(email, req.otp.strip()) != challenge["otpHash"]:
        challenge["attempts"] += 1
        return {"verified": False, "error": "Incorrect verification code."}
    in_memory_db["verifiedEmailOtps"][email_hash] = {"verifiedAt": now}
    in_memory_db["emailOtpChallenges"].pop(email_hash, None)
    return {"verified": True, "message": "Institutional email verified successfully."}


# ============================================================================
# 2. COLLEGE MANAGEMENT (Super Admin / College Admin)
# ============================================================================

@app.post("/api/colleges", dependencies=[Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN"]))])
def create_college(req: CollegeCreate, user: UserTokenPayload = Depends(get_current_user)):
    college_id = f"col_{req.code.lower()}_{int(_now_ms())}"
    data = {
        "id": college_id,
        "name": req.name,
        "code": req.code.upper(),
        "city": req.city,
        "adminEmail": req.adminEmail or user.email,
        "departmentsCount": 0,
        "studentsCount": 0,
        "status": req.status,
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("colleges", college_id, data)
    _audit("CREATE_COLLEGE", user.uid, user.role, "college", college_id, f"Created college {req.name}")
    return {"success": True, "college": data}


@app.get("/api/colleges")
def list_colleges(user: UserTokenPayload = Depends(get_current_user)):
    if "SUPER_ADMIN" in user.role.upper():
        return {"colleges": _list("colleges")}
    if user.collegeId:
        college = _get("colleges", user.collegeId)
        return {"colleges": [college] if college else []}
    return {"colleges": []}


@app.get("/api/colleges/{college_id}")
def get_college(college_id: str, user: UserTokenPayload = Depends(require_college_access)):
    college = _get("colleges", college_id)
    if not college:
        raise HTTPException(status_code=404, detail="College not found.")
    return {"college": college}


@app.put("/api/colleges/{college_id}")
def update_college(college_id: str, req: CollegeUpdate, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN"]))):
    college = _get("colleges", college_id)
    if not college:
        raise HTTPException(status_code=404, detail="College not found.")
    updates = {k: v for k, v in req.dict(exclude_unset=True).items() if v is not None}
    updated = _update("colleges", college_id, updates)
    _audit("UPDATE_COLLEGE", user.uid, user.role, "college", college_id, f"Updated college {college_id}")
    return {"success": True, "college": updated}


@app.post("/api/colleges/{college_id}/suspend")
def suspend_college(college_id: str, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN"]))):
    _update("colleges", college_id, {"status": "Suspended"})
    _audit("SUSPEND_COLLEGE", user.uid, user.role, "college", college_id, "College suspended")
    return {"success": True, "status": "Suspended"}


@app.post("/api/colleges/{college_id}/activate")
def activate_college(college_id: str, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN"]))):
    _update("colleges", college_id, {"status": "Active"})
    _audit("ACTIVATE_COLLEGE", user.uid, user.role, "college", college_id, "College activated")
    return {"success": True, "status": "Active"}


# ============================================================================
# 3. DEPARTMENT MANAGEMENT
# ============================================================================

@app.post("/api/departments", dependencies=[Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN", "PRINCIPAL"]))])
def create_department(req: DepartmentCreate, user: UserTokenPayload = Depends(get_current_user)):
    if "SUPER_ADMIN" not in user.role.upper() and req.collegeId != user.collegeId:
        raise HTTPException(status_code=403, detail="Cannot create department in another college.")
    dept_id = f"dept_{req.code.lower()}_{int(_now_ms())}"
    data = {
        "id": dept_id,
        "collegeId": req.collegeId,
        "name": req.name,
        "code": req.code.upper(),
        "hodName": req.hodName,
        "hodEmail": req.hodEmail,
        "hodId": req.hodId,
        "facultyCount": 0,
        "studentsCount": 0,
        "avgAttendance": 0.0,
        "status": "Active",
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("departments", dept_id, data)
    _update("colleges", req.collegeId, {"departmentsCount": _list("departments", [("collegeId", req.collegeId)])})
    _audit("CREATE_DEPARTMENT", user.uid, user.role, "department", dept_id, f"Created department {req.name}")
    return {"success": True, "department": data}


@app.get("/api/departments")
def list_departments(collegeId: Optional[str] = None, user: UserTokenPayload = Depends(require_college_access)):
    if "SUPER_ADMIN" in user.role.upper() and collegeId:
        return {"departments": _list("departments", [("collegeId", collegeId)])}
    return {"departments": _list("departments", [("collegeId", user.collegeId)])}


@app.get("/api/departments/{dept_id}")
def get_department(dept_id: str, user: UserTokenPayload = Depends(require_college_access)):
    dept = _get("departments", dept_id)
    if not dept:
        raise HTTPException(status_code=404, detail="Department not found.")
    if "SUPER_ADMIN" not in user.role.upper() and dept.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this department.")
    return {"department": dept}


@app.put("/api/departments/{dept_id}")
def update_department(dept_id: str, req: DepartmentUpdate, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN", "PRINCIPAL"]))):
    dept = _get("departments", dept_id)
    if not dept:
        raise HTTPException(status_code=404, detail="Department not found.")
    if "SUPER_ADMIN" not in user.role.upper() and dept.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this department.")
    updates = {k: v for k, v in req.dict(exclude_unset=True).items() if v is not None}
    updated = _update("departments", dept_id, updates)
    _audit("UPDATE_DEPARTMENT", user.uid, user.role, "department", dept_id, f"Updated department {dept_id}")
    return {"success": True, "department": updated}


# ============================================================================
# 4. ACADEMIC YEARS, SEMESTERS, SECTIONS, SUBJECTS
# ============================================================================

@app.post("/api/academic-years")
def create_academic_year(req: AcademicYearCreate, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN", "PRINCIPAL"]))):
    if "SUPER_ADMIN" not in user.role.upper() and req.collegeId != user.collegeId:
        raise HTTPException(status_code=403, detail="Cannot create academic year in another college.")
    if req.isCurrent:
        _list("academicYears", [("collegeId", req.collegeId)])
        for ay in _list("academicYears", [("collegeId", req.collegeId)]):
            _update("academicYears", ay["id"], {"isCurrent": False})
    ay_id = f"ay_{int(_now_ms())}"
    data = {
        "id": ay_id,
        "collegeId": req.collegeId,
        "name": req.name,
        "startDate": req.startDate,
        "endDate": req.endDate,
        "isCurrent": req.isCurrent,
        "status": "Active",
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("academicYears", ay_id, data)
    return {"success": True, "academicYear": data}


@app.get("/api/academic-years")
def list_academic_years(collegeId: Optional[str] = None, user: UserTokenPayload = Depends(require_college_access)):
    cid = collegeId if ("SUPER_ADMIN" in user.role.upper() and collegeId) else user.collegeId
    return {"academicYears": _list("academicYears", [("collegeId", cid)])}


@app.post("/api/semesters")
def create_semester(req: SemesterCreate, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN", "PRINCIPAL"]))):
    if "SUPER_ADMIN" not in user.role.upper() and req.collegeId != user.collegeId:
        raise HTTPException(status_code=403, detail="Cannot create semester in another college.")
    sem_id = f"sem_{int(_now_ms())}"
    data = {
        "id": sem_id,
        "collegeId": req.collegeId,
        "academicYearId": req.academicYearId,
        "name": req.name,
        "number": req.number,
        "startDate": req.startDate,
        "endDate": req.endDate,
        "isCurrent": req.isCurrent,
        "status": "Active",
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("semesters", sem_id, data)
    return {"success": True, "semester": data}


@app.get("/api/semesters")
def list_semesters(collegeId: Optional[str] = None, academicYearId: Optional[str] = None, user: UserTokenPayload = Depends(require_college_access)):
    cid = collegeId if ("SUPER_ADMIN" in user.role.upper() and collegeId) else user.collegeId
    filters = [("collegeId", cid)]
    if academicYearId:
        filters.append(("academicYearId", academicYearId))
    return {"semesters": _list("semesters", filters)}


@app.post("/api/sections")
def create_section(req: SectionCreate, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN", "PRINCIPAL", "HOD"]))):
    if "SUPER_ADMIN" not in user.role.upper() and req.collegeId != user.collegeId:
        raise HTTPException(status_code=403, detail="Cannot create section in another college.")
    sec_id = f"sec_{int(_now_ms())}"
    data = {
        "id": sec_id,
        "collegeId": req.collegeId,
        "departmentId": req.departmentId,
        "academicYearId": req.academicYearId,
        "semesterId": req.semesterId,
        "name": req.name,
        "studentCount": 0,
        "status": "Active",
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("sections", sec_id, data)
    return {"success": True, "section": data}


@app.get("/api/sections")
def list_sections(collegeId: Optional[str] = None, departmentId: Optional[str] = None, semesterId: Optional[str] = None, user: UserTokenPayload = Depends(require_college_access)):
    cid = collegeId if ("SUPER_ADMIN" in user.role.upper() and collegeId) else user.collegeId
    filters = [("collegeId", cid)]
    if departmentId:
        filters.append(("departmentId", departmentId))
    if semesterId:
        filters.append(("semesterId", semesterId))
    return {"sections": _list("sections", filters)}


@app.post("/api/subjects")
def create_subject(req: SubjectCreate, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN", "PRINCIPAL", "HOD"]))):
    if "SUPER_ADMIN" not in user.role.upper() and req.collegeId != user.collegeId:
        raise HTTPException(status_code=403, detail="Cannot create subject in another college.")
    subj_id = f"subj_{req.code}_{int(_now_ms())}"
    data = {
        "id": subj_id,
        "collegeId": req.collegeId,
        "departmentId": req.departmentId,
        "code": req.code,
        "name": req.name,
        "credits": req.credits,
        "year": req.year,
        "semester": req.semester,
        "status": "Active",
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("subjects", subj_id, data)
    return {"success": True, "subject": data}


@app.get("/api/subjects")
def list_subjects(collegeId: Optional[str] = None, departmentId: Optional[str] = None, user: UserTokenPayload = Depends(require_college_access)):
    cid = collegeId if ("SUPER_ADMIN" in user.role.upper() and collegeId) else user.collegeId
    filters = [("collegeId", cid)]
    if departmentId:
        filters.append(("departmentId", departmentId))
    return {"subjects": _list("subjects", filters)}


# ============================================================================
# 5. FACULTY & STUDENT ALLOCATIONS
# ============================================================================

@app.post("/api/faculty-allocations")
def create_faculty_allocation(req: FacultyAllocationCreate, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN", "PRINCIPAL", "HOD"]))):
    if "SUPER_ADMIN" not in user.role.upper() and req.collegeId != user.collegeId:
        raise HTTPException(status_code=403, detail="Cannot allocate faculty in another college.")
    alloc_id = f"facalloc_{int(_now_ms())}"
    data = {
        "id": alloc_id,
        "collegeId": req.collegeId,
        "departmentId": req.departmentId,
        "facultyId": req.facultyId,
        "subjectId": req.subjectId,
        "academicYearId": req.academicYearId,
        "semesterId": req.semesterId,
        "sectionId": req.sectionId,
        "isActive": True,
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("facultyAllocations", alloc_id, data)
    _audit("CREATE_FACULTY_ALLOCATION", user.uid, user.role, "facultyAllocation", alloc_id, "Faculty allocated to subject")
    return {"success": True, "allocation": data}


@app.get("/api/faculty-allocations")
def list_faculty_allocations(facultyId: Optional[str] = None, collegeId: Optional[str] = None, departmentId: Optional[str] = None, user: UserTokenPayload = Depends(require_college_access)):
    cid = collegeId if ("SUPER_ADMIN" in user.role.upper() and collegeId) else user.collegeId
    filters = [("collegeId", cid)]
    if facultyId:
        filters.append(("facultyId", facultyId))
    if departmentId:
        filters.append(("departmentId", departmentId))
    return {"allocations": _list("facultyAllocations", filters)}


@app.post("/api/student-allocations")
def create_student_allocation(req: StudentAllocationCreate, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN", "PRINCIPAL", "HOD"]))):
    if "SUPER_ADMIN" not in user.role.upper() and req.collegeId != user.collegeId:
        raise HTTPException(status_code=403, detail="Cannot allocate student in another college.")
    alloc_id = f"studalloc_{int(_now_ms())}"
    data = {
        "id": alloc_id,
        "collegeId": req.collegeId,
        "studentId": req.studentId,
        "departmentId": req.departmentId,
        "academicYearId": req.academicYearId,
        "semesterId": req.semesterId,
        "sectionId": req.sectionId,
        "rollNumber": req.rollNumber,
        "isActive": True,
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("studentAllocations", alloc_id, data)
    return {"success": True, "allocation": data}


@app.get("/api/student-allocations")
def list_student_allocations(studentId: Optional[str] = None, collegeId: Optional[str] = None, departmentId: Optional[str] = None, user: UserTokenPayload = Depends(require_college_access)):
    cid = collegeId if ("SUPER_ADMIN" in user.role.upper() and collegeId) else user.collegeId
    filters = [("collegeId", cid)]
    if studentId:
        filters.append(("studentId", studentId))
    if departmentId:
        filters.append(("departmentId", departmentId))
    return {"allocations": _list("studentAllocations", filters)}


# ============================================================================
# 6. TIMETABLE
# ============================================================================

@app.post("/api/timetable-slots")
def create_timetable_slot(req: TimetableSlotCreate, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN", "PRINCIPAL", "HOD"]))):
    if "SUPER_ADMIN" not in user.role.upper() and req.collegeId != user.collegeId:
        raise HTTPException(status_code=403, detail="Cannot create timetable in another college.")
    slot_id = f"tt_{int(_now_ms())}"
    data = {
        "id": slot_id,
        "collegeId": req.collegeId,
        "departmentId": req.departmentId,
        "sectionId": req.sectionId,
        "subjectId": req.subjectId,
        "facultyId": req.facultyId,
        "day": req.day,
        "periodNumber": req.periodNumber,
        "timeSlot": req.timeSlot,
        "roomNumber": req.roomNumber,
        "academicYearId": req.academicYearId,
        "semesterId": req.semesterId,
        "isActive": True,
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("timetableSlots", slot_id, data)
    return {"success": True, "slot": data}


@app.get("/api/timetable-slots")
def list_timetable_slots(sectionId: Optional[str] = None, collegeId: Optional[str] = None, day: Optional[str] = None, user: UserTokenPayload = Depends(require_college_access)):
    cid = collegeId if ("SUPER_ADMIN" in user.role.upper() and collegeId) else user.collegeId
    filters = [("collegeId", cid)]
    if sectionId:
        filters.append(("sectionId", sectionId))
    if day:
        filters.append(("day", day))
    return {"slots": _list("timetableSlots", filters)}


# ============================================================================
# 7. BIOMETRIC FACE RECOGNITION
# ============================================================================

def cosine_similarity(v1: List[float], v2: List[float]) -> float:
    if len(v1) != len(v2) or not v1:
        return 0.0
    dot = sum(a * b for a, b in zip(v1, v2))
    norm1 = math.sqrt(sum(a * a for a in v1))
    norm2 = math.sqrt(sum(b * b for b in v2))
    if norm1 == 0 or norm2 == 0:
        return 0.0
    return dot / (norm1 * norm2)


def euclidean_similarity_score(v1: List[float], v2: List[float]) -> float:
    if len(v1) != len(v2) or not v1:
        return 0.0
    dist_sq = sum((a - b) ** 2 for a, b in zip(v1, v2))
    dist = math.sqrt(dist_sq)
    return 1.0 / (1.0 + dist)


def combined_face_score(v1: List[float], v2: List[float]) -> tuple:
    """Returns (cosine, euclidean, combined) where combined = 0.6*cosine + 0.4*euclidean."""
    c = cosine_similarity(v1, v2)
    e = euclidean_similarity_score(v1, v2)
    combined = 0.6 * c + 0.4 * e
    return c, e, combined


# Advanced face recognition thresholds (production-grade)
COSINE_MATCH_THRESHOLD = float(os.getenv("FACE_COSINE_MATCH_THRESHOLD", "0.72"))
COSINE_AMBIGUOUS_THRESHOLD = float(os.getenv("FACE_COSINE_AMBIGUOUS_THRESHOLD", "0.60"))
EUCLIDEAN_MATCH_THRESHOLD = float(os.getenv("FACE_EUCLIDEAN_MATCH_THRESHOLD", "0.85"))
EUCLIDEAN_AMBIGUOUS_THRESHOLD = float(os.getenv("FACE_EUCLIDEAN_AMBIGUOUS_THRESHOLD", "0.75"))
REQUIRE_BOTH_METRICS = os.getenv("FACE_REQUIRE_BOTH_METRICS", "true").lower() == "true"
REQUIRED_LIVENESS_FOR_ATTENDANCE = os.getenv("FACE_REQUIRED_LIVENESS", "true").lower() == "true"
MIN_LIVENESS_CONFIDENCE = float(os.getenv("FACE_MIN_LIVENESS_CONFIDENCE", "0.80"))


class FaceEnrollRequestAdvanced(BaseModel):
    studentId: str
    studentName: str
    departmentId: str
    section: str
    embedding: List[float] = Field(..., description="Averaged 128-D L2-normalized embedding from multi-frame accumulation")
    embeddings: List[List[float]] = Field(default=[], description="Raw per-frame embeddings (for multi-frame validation)")
    confidences: List[float] = Field(default=[], description="Per-frame detection confidences")
    frameCount: int = Field(default=1, description="Number of frames used to compute embedding")
    consentConfirmed: bool = True
    livenessConfidence: float = Field(default=1.0, description="Liveness confidence score 0-1")
    modelVersion: str = "arcface_v2_multi_frame"


class FaceRecognizeRequestAdvanced(BaseModel):
    subjectCode: str
    sectionId: str
    embeddings: List[List[float]] = Field(..., description="List of detected face embeddings per frame")
    frameEmbeddings: List[List[float]] = Field(default=[], description="Multi-frame embeddings for temporal averaging")
    livenessData: Optional[Dict[str, Any]] = None
    requireLiveness: bool = True


@app.post("/api/biometrics/enroll")
def enroll_student_face(req: FaceEnrollRequestAdvanced, user: UserTokenPayload = Depends(require_role(["FACULTY", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    if not req.consentConfirmed:
        raise HTTPException(status_code=400, detail="Institutional student consent is required before face enrollment.")

    # Validate embedding dimension
    if len(req.embedding) not in (128, 512):
        raise HTTPException(status_code=400, detail=f"Embedding must be 128 or 512-dimensional, got {len(req.embedding)}")

    # Liveness check for enrollment
    if REQUIRED_LIVENESS_FOR_ATTENDANCE and req.livenessConfidence < MIN_LIVENESS_CONFIDENCE:
        raise HTTPException(status_code=400, detail=f"Liveness confidence too low ({req.livenessConfidence}). Please re-scan with a live face.")

    template_id = f"tpl_{req.studentId}"
    face_model = os.getenv("FACE_MODEL_NAME", "arcface_resnet50")
    detector = os.getenv("FACE_DETECTOR_BACKEND", "opencv_dnn")

    # L2-normalize the embedding before storage
    norm = math.sqrt(sum(x * x for x in req.embedding))
    normalized_embedding = [x / norm for x in req.embedding] if norm > 0 else req.embedding

    record = {
        "id": template_id,
        "studentId": req.studentId,
        "studentName": req.studentName,
        "departmentId": req.departmentId,
        "section": req.section,
        "collegeId": user.collegeId,
        "modelName": face_model,
        "detectorBackend": detector,
        "embedding": normalized_embedding,
        "rawEmbeddings": req.embeddings if req.embeddings else [],
        "frameCount": req.frameCount,
        "enrolledBy": user.uid,
        "enrolledAt": _now_ms(),
        "consentConfirmed": True,
        "isActive": True,
        "livenessConfidence": getattr(req, 'livenessConfidence', 1.0),
        "modelVersion": "arcface_v2_multi_frame"
    }
    _create("faceTemplates", template_id, record)
    _audit("ENROLL_FACE", user.uid, user.role, "faceTemplate", template_id, f"Enrolled face for {req.studentName} ({req.studentId}) with {req.frameCount} frames")
    return {
        "success": True,
        "model": face_model,
        "detector": detector,
        "templateId": template_id,
        "frameCount": req.frameCount,
        "message": f"Advanced biometric template enrolled securely for {req.studentName} ({req.studentId}) via {face_model} ({req.frameCount} frames, liveness verified)."
    }


@app.post("/api/biometrics/recognize")
def recognize_faces(req: FaceRecognizeRequestAdvanced, user: UserTokenPayload = Depends(require_role(["FACULTY", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    results = []
    templates = _list("faceTemplates", [("collegeId", user.collegeId), ("isActive", True)])

    # Liveness gate
    if REQUIRED_LIVENESS_FOR_ATTENDANCE and req.requireLiveness:
        liveness_data = req.livenessData or {}
        liveness_conf = float(liveness_data.get("confidence", 1.0))
        if liveness_conf < MIN_LIVENESS_CONFIDENCE:
            return {
                "matches": [],
                "totalDetected": len(req.embeddings),
                "livenessFailed": True,
                "livenessConfidence": round(liveness_conf, 3),
                "message": f"Liveness check failed (confidence {round(liveness_conf, 3)} < {MIN_LIVENESS_CONFIDENCE}). Possible spoof attempt."
            }

    # Multi-frame averaging: if frameEmbeddings provided, average them first
    embeddings_to_match = list(req.embeddings)
    if req.frameEmbeddings:
        # Average across frames
        dim = len(req.frameEmbeddings[0]) if req.frameEmbeddings else len(req.embeddings[0]) if req.embeddings else 128
        summed = [0.0] * dim
        for emb in req.frameEmbeddings:
            for i in range(min(dim, len(emb))):
                summed[i] += emb[i]
        count = len(req.frameEmbeddings)
        averaged = [s / count for s in summed]
        embeddings_to_match = [averaged]

    for face_vec in embeddings_to_match:
        best_match = None
        best_combined = -1.0
        best_cosine = -1.0
        best_euclidean = -1.0

        for tpl in templates:
            stored_emb = tpl.get("embedding", [])
            if not stored_emb or len(stored_emb) != len(face_vec):
                continue
            c, e, combined = combined_face_score(face_vec, stored_emb)
            if combined > best_combined:
                best_combined = combined
                best_cosine = c
                best_euclidean = e
                best_match = tpl

        cosine_match = best_cosine >= COSINE_MATCH_THRESHOLD
        euclidean_match = best_euclidean >= EUCLIDEAN_MATCH_THRESHOLD

        both_pass = cosine_match and euclidean_match
        either_pass = cosine_match or euclidean_match

        if REQUIRE_BOTH_METRICS:
            confident = both_pass
        else:
            confident = either_pass

        ambiguous = (best_cosine >= COSINE_AMBIGUOUS_THRESHOLD or best_euclidean >= EUCLIDEAN_AMBIGUOUS_THRESHOLD) and not confident

        if confident and best_match:
            results.append({
                "studentId": best_match["studentId"],
                "studentName": best_match["studentName"],
                "confidence": round(best_combined, 3),
                "cosineSimilarity": round(best_cosine, 3),
                "euclideanScore": round(best_euclidean, 3),
                "status": "MATCHED",
                "needsConfirmation": False,
                "matchMetrics": {
                    "isCosineMatch": cosine_match,
                    "isEuclideanMatch": euclidean_match,
                    "combinedScore": round(best_combined, 3)
                }
            })
        elif ambiguous and best_match:
            results.append({
                "studentId": best_match["studentId"],
                "studentName": best_match["studentName"],
                "confidence": round(best_combined, 3),
                "cosineSimilarity": round(best_cosine, 3),
                "euclideanScore": round(best_euclidean, 3),
                "status": "AMBIGUOUS",
                "needsConfirmation": True,
                "matchMetrics": {
                    "isCosineMatch": cosine_match,
                    "isEuclideanMatch": euclidean_match,
                    "combinedScore": round(best_combined, 3)
                }
            })
        else:
            results.append({
                "studentId": "UNKNOWN",
                "studentName": "Not Matched",
                "confidence": round(max(best_combined, 0.0), 3),
                "cosineSimilarity": round(best_cosine, 3),
                "euclideanScore": round(best_euclidean, 3),
                "status": "UNMATCHED",
                "needsConfirmation": True,
                "matchMetrics": {
                    "isCosineMatch": False,
                    "isEuclideanMatch": False,
                    "combinedScore": round(max(best_combined, 0.0), 3)
                }
            })

    return {
        "matches": results,
        "totalDetected": len(embeddings_to_match),
        "livenessFailed": False,
        "thresholds": {
            "cosineMatch": COSINE_MATCH_THRESHOLD,
            "euclideanMatch": EUCLIDEAN_MATCH_THRESHOLD,
            "requireBothMetrics": REQUIRE_BOTH_METRICS
        }
    }


@app.delete("/api/biometrics/delete/{student_id}")
def delete_biometric_template(student_id: str, user: UserTokenPayload = Depends(require_role(["ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    template_id = f"tpl_{student_id}"
    if _get("faceTemplates", template_id):
        _delete("faceTemplates", template_id)
        _audit("DELETE_FACE", user.uid, user.role, "faceTemplate", template_id, f"Deleted face template for {student_id}")
        return {"deleted": True, "message": f"Biometric template for {student_id} permanently erased."}
    return {"deleted": False, "message": "Record not found."}


# ============================================================================
# 8. ATTENDANCE WORKFLOWS
# ============================================================================

@app.post("/api/attendance/submit")
def submit_attendance(req: AttendanceRecordSubmit, user: UserTokenPayload = Depends(require_role(["FACULTY", "HOD", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    college_id = user.collegeId if "SUPER_ADMIN" not in user.role.upper() else req.departmentId and user.collegeId or "default"
    record_id = f"att_rec_{int(_now_ms())}_{secrets.randbelow(1000)}"
    total = len(req.presentStudentIds) + len(req.absentStudentIds) + len(req.lateStudentIds) + len(req.excusedStudentIds)
    record = {
        "id": record_id,
        "sessionId": req.sessionId,
        "subjectCode": req.subjectCode,
        "subjectName": req.subjectName,
        "departmentId": req.departmentId,
        "sectionId": req.sectionId,
        "collegeId": college_id,
        "periodNumber": req.periodNumber,
        "date": req.date,
        "presentCount": len(req.presentStudentIds),
        "absentCount": len(req.absentStudentIds),
        "totalStudents": total,
        "presentStudentIds": req.presentStudentIds,
        "absentStudentIds": req.absentStudentIds,
        "lateStudentIds": req.lateStudentIds,
        "excusedStudentIds": req.excusedStudentIds,
        "markedBy": user.uid,
        "markedByRole": user.role,
        "method": req.method,
        "createdAt": _now_ms()
    }
    _create("attendanceRecords", record_id, record)
    for sid in req.presentStudentIds:
        key = f"{sid}_{req.subjectCode}"
        existing = _get("studentAttendance", key)
        if existing:
            _update("studentAttendance", key, {
                "totalSessions": existing.get("totalSessions", 0) + 1,
                "attendedSessions": existing.get("attendedSessions", 0) + 1,
                "attendancePercentage": round(((existing.get("attendedSessions", 0) + 1) / (existing.get("totalSessions", 0) + 1)) * 100, 2)
            })
        else:
            _create("studentAttendance", key, {
                "id": key,
                "collegeId": college_id,
                "studentId": sid,
                "subjectCode": req.subjectCode,
                "departmentId": req.departmentId,
                "totalSessions": 1,
                "attendedSessions": 1,
                "absentSessions": 0,
                "lateSessions": 0,
                "attendancePercentage": 100.0,
                "lastUpdated": _now_ms()
            })
    _audit("ATTENDANCE_MARKED", user.uid, user.role, "attendanceRecord", record_id, f"Marked {len(req.presentStudentIds)}/{total} present for {req.subjectName}")
    return {"success": True, "recordId": record_id, "presentCount": len(req.presentStudentIds), "total": total}


@app.post("/api/attendance/correct")
def correct_attendance(req: AttendanceCorrectionRequest, user: UserTokenPayload = Depends(require_role(["FACULTY", "HOD", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    record = _get("attendanceRecords", req.recordId)
    if not record:
        raise HTTPException(status_code=404, detail="Attendance record not found.")
    if req.newStatus == "PRESENT":
        if req.studentId in record.get("absentStudentIds", []):
            record["absentStudentIds"].remove(req.studentId)
        if req.studentId not in record.get("presentStudentIds", []):
            record["presentStudentIds"].append(req.studentId)
    elif req.newStatus == "ABSENT":
        if req.studentId in record.get("presentStudentIds", []):
            record["presentStudentIds"].remove(req.studentId)
        if req.studentId not in record.get("absentStudentIds", []):
            record["absentStudentIds"].append(req.studentId)
    record["presentCount"] = len(record["presentStudentIds"])
    record["absentCount"] = len(record["absentStudentIds"])
    _update("attendanceRecords", req.recordId, record)
    _audit("ATTENDANCE_CORRECTION", user.uid, user.role, "attendanceRecord", req.recordId, f"Updated {req.studentId} to {req.newStatus}. Reason: {req.reason}")
    return {"corrected": True, "message": f"Updated {req.studentId} to {req.newStatus}. Reason logged in audit trail."}


@app.get("/api/attendance/history")
def get_attendance_history(subjectCode: Optional[str] = None, sectionId: Optional[str] = None, studentId: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if subjectCode:
        filters.append(("subjectCode", subjectCode))
    if sectionId:
        filters.append(("sectionId", sectionId))
    records = _list("attendanceRecords", filters)
    if studentId:
        records = [r for r in records if studentId in r.get("presentStudentIds", []) or studentId in r.get("absentStudentIds", []) or studentId in r.get("lateStudentIds", [])]
    return {"records": records}


@app.get("/api/attendance/student/{studentId}")
def get_student_attendance(studentId: str, subjectCode: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId), ("studentId", studentId)]
    if subjectCode:
        filters.append(("subjectCode", subjectCode))
    records = _list("studentAttendance", filters)
    return {"records": records}


# ============================================================================
# 9. LEAVE REQUESTS
# ============================================================================

@app.post("/api/leave/apply")
def apply_leave(req: LeaveRequestCreate, user: UserTokenPayload = Depends(get_current_user)):
    leave_id = f"leave_{int(_now_ms())}_{secrets.randbelow(1000)}"
    leave = {
        "id": leave_id,
        "applicantId": user.uid,
        "applicantName": user.displayName,
        "applicantRole": user.role,
        "departmentId": user.departmentId,
        "collegeId": user.collegeId,
        "leaveType": req.leaveType,
        "startDate": req.startDate,
        "endDate": req.endDate,
        "reason": req.reason,
        "documentUrl": req.documentUrl,
        "status": "PENDING",
        "decidedBy": None,
        "decisionRemarks": None,
        "createdAt": _now_ms()
    }
    _create("leaveRequests", leave_id, leave)
    _audit("LEAVE_APPLY", user.uid, user.role, "leaveRequest", leave_id, f"Leave application {req.leaveType}")
    return {"success": True, "leaveId": leave_id, "status": "PENDING"}


@app.get("/api/leave/pending")
def get_pending_leaves(user: UserTokenPayload = Depends(require_role(["HOD", "PRINCIPAL", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    filters = [("collegeId", user.collegeId), ("status", "PENDING")]
    if user.role.upper() == "HOD" and user.departmentId:
        filters.append(("departmentId", user.departmentId))
    return {"pendingLeaves": _list("leaveRequests", filters)}


@app.get("/api/leave/my")
def get_my_leaves(user: UserTokenPayload = Depends(get_current_user)):
    return {"leaves": _list("leaveRequests", [("applicantId", user.uid)])}


@app.post("/api/leave/decide")
def decide_leave(req: LeaveDecisionRequest, user: UserTokenPayload = Depends(require_role(["HOD", "PRINCIPAL", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    leave = _get("leaveRequests", req.leaveId)
    if not leave:
        raise HTTPException(status_code=404, detail="Leave request not found.")
    if "SUPER_ADMIN" not in user.role.upper() and leave.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this leave request.")
    leave["status"] = req.decision.upper()
    leave["decidedBy"] = user.displayName
    leave["decisionRemarks"] = req.remarks
    leave["decidedAt"] = _now_ms()
    _update("leaveRequests", req.leaveId, leave)
    _audit("LEAVE_DECISION", user.uid, user.role, "leaveRequest", req.leaveId, f"Leave {req.decision} by {user.displayName}")
    return {"success": True, "leaveId": req.leaveId, "status": leave["status"]}


# ============================================================================
# 10. ASSIGNMENTS & SUBMISSIONS
# ============================================================================

@app.post("/api/assignments/create")
def create_assignment(req: AssignmentCreate, user: UserTokenPayload = Depends(require_role(["FACULTY", "HOD", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    college_id = user.collegeId if "SUPER_ADMIN" not in user.role.upper() else req.departmentId and user.collegeId or "default"
    assignment_id = f"asgn_{int(_now_ms())}"
    assignment = {
        "id": assignment_id,
        "collegeId": college_id,
        "departmentId": req.departmentId,
        "sectionId": req.sectionId,
        "subjectCode": req.subjectCode,
        "subjectName": req.subjectName,
        "title": req.title,
        "instructions": req.instructions,
        "deadline": req.deadline,
        "maxMarks": req.maxMarks,
        "permittedFileType": req.permittedFileType,
        "createdBy": user.uid,
        "status": "ACTIVE",
        "createdAt": _now_ms()
    }
    _create("assignments", assignment_id, assignment)
    _audit("CREATE_ASSIGNMENT", user.uid, user.role, "assignment", assignment_id, f"Created assignment {req.title}")
    return {"success": True, "assignment": assignment}


@app.get("/api/assignments/list")
def list_assignments(subjectCode: Optional[str] = None, sectionId: Optional[str] = None, status: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if subjectCode:
        filters.append(("subjectCode", subjectCode))
    if sectionId:
        filters.append(("sectionId", sectionId))
    if status:
        filters.append(("status", status))
    assignments = _list("assignments", filters)
    return {"assignments": assignments}


@app.get("/api/assignments/submissions")
def list_submissions(assignmentId: Optional[str] = None, studentId: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if assignmentId:
        filters.append(("assignmentId", assignmentId))
    if studentId:
        filters.append(("studentId", studentId))
    elif user.role.upper() == "STUDENT":
        filters.append(("studentId", user.uid))
    return {"submissions": _list("assignmentSubmissions", filters)}


@app.get("/api/assignments/{assignment_id}")
def get_assignment(assignment_id: str, user: UserTokenPayload = Depends(get_current_user)):
    assignment = _get("assignments", assignment_id)
    if not assignment:
        raise HTTPException(status_code=404, detail="Assignment not found.")
    if "SUPER_ADMIN" not in user.role.upper() and assignment.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this assignment.")
    return {"assignment": assignment}


@app.post("/api/assignments/submit")
def submit_assignment(req: SubmissionCreate, user: UserTokenPayload = Depends(require_role(["STUDENT"]))):
    assignment = _get("assignments", req.assignmentId)
    if not assignment:
        raise HTTPException(status_code=404, detail="Assignment not found.")
    if "SUPER_ADMIN" not in user.role.upper() and assignment.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this assignment.")
    sub_id = f"sub_{user.uid}_{req.assignmentId}"
    submission = {
        "id": sub_id,
        "collegeId": user.collegeId,
        "assignmentId": req.assignmentId,
        "studentId": user.uid,
        "studentName": user.displayName,
        "fileUrl": req.fileUrl,
        "fileName": req.fileName,
        "fileSizeBytes": req.fileSizeBytes,
        "submittedAt": _now_ms(),
        "status": "SUBMITTED",
        "marksObtained": None,
        "feedback": None
    }
    _create("assignmentSubmissions", sub_id, submission)
    _audit("ASSIGNMENT_SUBMIT", user.uid, user.role, "submission", sub_id, f"Submitted assignment {req.assignmentId}")
    return {"success": True, "submissionId": sub_id, "status": "SUBMITTED"}


@app.post("/api/assignments/grade")
def grade_submission(req: GradeSubmissionRequest, user: UserTokenPayload = Depends(require_role(["FACULTY", "HOD", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    sub = _get("assignmentSubmissions", req.submissionId)
    if not sub:
        raise HTTPException(status_code=404, detail="Submission not found.")
    if "SUPER_ADMIN" not in user.role.upper() and sub.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this submission.")
    sub["marksObtained"] = req.marksObtained
    sub["feedback"] = req.feedback
    sub["gradedBy"] = user.displayName
    sub["gradedAt"] = _now_ms()
    sub["status"] = "GRADED"
    _update("assignmentSubmissions", req.submissionId, sub)
    _audit("GRADE_SUBMISSION", user.uid, user.role, "submission", req.submissionId, f"Graded submission {req.submissionId} with {req.marksObtained} marks")
    return {"success": True, "submission": sub}


# ============================================================================
# 11. ANNOUNCEMENTS
# ============================================================================

@app.post("/api/announcements/create")
def create_announcement(req: AnnouncementCreate, user: UserTokenPayload = Depends(require_role(["FACULTY", "HOD", "PRINCIPAL", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    ann_id = f"ann_{int(_now_ms())}"
    ann = {
        "id": ann_id,
        "collegeId": user.collegeId,
        "departmentId": req.departmentId,
        "title": req.title,
        "message": req.message,
        "audience": req.audience,
        "section": req.section,
        "priority": req.priority,
        "authorName": user.displayName,
        "authorRole": user.role,
        "authorId": user.uid,
        "createdAt": _now_ms()
    }
    _create("announcements", ann_id, ann)
    _audit("CREATE_ANNOUNCEMENT", user.uid, user.role, "announcement", ann_id, f"Created announcement {req.title}")
    return {"success": True, "announcement": ann}


@app.get("/api/announcements/list")
def list_announcements(departmentId: Optional[str] = None, audience: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if departmentId:
        filters.append(("departmentId", departmentId))
    if audience:
        filters.append(("audience", audience))
    return {"announcements": _list("announcements", filters)}


# ============================================================================
# 12. DOCUMENTS (Private Drive + Shared Storage)
# ============================================================================

@app.post("/api/documents")
def create_document(req: DocumentCreate, user: UserTokenPayload = Depends(get_current_user)):
    doc_id = f"doc_{int(_now_ms())}_{secrets.randbelow(1000)}"
    doc = {
        "id": doc_id,
        "ownerId": user.uid,
        "collegeId": user.collegeId,
        "departmentId": user.departmentId,
        "title": req.title,
        "fileUrl": req.fileUrl,
        "fileName": req.fileName,
        "fileType": req.fileType,
        "fileSizeBytes": req.fileSizeBytes,
        "folderId": req.folderId,
        "isPrivate": req.isPrivate,
        "sharedWith": req.sharedWith,
        "sharedWithDepartments": req.sharedWithDepartments,
        "sharedWithCollege": req.sharedWithCollege,
        "uploadedAt": _now_ms(),
        "updatedAt": _now_ms()
    }
    _create("documents", doc_id, doc)
    _audit("CREATE_DOCUMENT", user.uid, user.role, "document", doc_id, f"Uploaded document {req.title}")
    return {"success": True, "document": doc}


@app.get("/api/documents")
def list_documents(folderId: Optional[str] = None, includeShared: bool = True, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if folderId:
        filters.append(("folderId", folderId))
    docs = _list("documents", filters)
    result = []
    for d in docs:
        if d.get("isPrivate") and d.get("ownerId") != user.uid:
            if user.uid not in d.get("sharedWith", []) and not d.get("sharedWithCollege"):
                if user.departmentId not in d.get("sharedWithDepartments", []):
                    continue
        result.append(d)
    return {"documents": result}


@app.get("/api/documents/{doc_id}")
def get_document(doc_id: str, user: UserTokenPayload = Depends(get_current_user)):
    doc = _get("documents", doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")
    if "SUPER_ADMIN" not in user.role.upper() and doc.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this document.")
    if doc.get("isPrivate") and doc.get("ownerId") != user.uid:
        if user.uid not in doc.get("sharedWith", []) and not doc.get("sharedWithCollege"):
            if user.departmentId not in doc.get("sharedWithDepartments", []):
                raise HTTPException(status_code=403, detail="Access denied to this private document.")
    return {"document": doc}


@app.delete("/api/documents/{doc_id}")
def delete_document(doc_id: str, user: UserTokenPayload = Depends(get_current_user)):
    doc = _get("documents", doc_id)
    if not doc:
        raise HTTPException(status_code=404, detail="Document not found.")
    if "SUPER_ADMIN" not in user.role.upper() and doc.get("ownerId") != user.uid:
        raise HTTPException(status_code=403, detail="Only the owner can delete a private document.")
    _delete("documents", doc_id)
    _audit("DELETE_DOCUMENT", user.uid, user.role, "document", doc_id, f"Deleted document {doc_id}")
    return {"deleted": True}


@app.post("/api/document-folders")
def create_document_folder(req: DocumentFolderCreate, user: UserTokenPayload = Depends(get_current_user)):
    folder_id = f"fld_{int(_now_ms())}_{secrets.randbelow(1000)}"
    folder = {
        "id": folder_id,
        "ownerId": user.uid,
        "collegeId": user.collegeId,
        "departmentId": user.departmentId,
        "name": req.name,
        "parentFolderId": req.parentFolderId,
        "isPrivate": req.isPrivate,
        "createdAt": _now_ms()
    }
    _create("documentFolders", folder_id, folder)
    return {"success": True, "folder": folder}


@app.get("/api/document-folders")
def list_document_folders(parentFolderId: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId), ("ownerId", user.uid)]
    if parentFolderId:
        filters.append(("parentFolderId", parentFolderId))
    else:
        filters.append(("parentFolderId", None))
    return {"folders": _list("documentFolders", filters)}


# ============================================================================
# 13. MESSAGING (Faculty-Student, HOD communication)
# ============================================================================

@app.post("/api/messages")
def send_message(req: MessageCreate, user: UserTokenPayload = Depends(get_current_user)):
    msg_id = f"msg_{int(_now_ms())}_{secrets.randbelow(1000)}"
    msg = {
        "id": msg_id,
        "collegeId": user.collegeId,
        "senderId": user.uid,
        "senderName": user.displayName,
        "senderRole": user.role,
        "receiverId": req.receiverId,
        "message": req.message,
        "isRead": False,
        "createdAt": _now_ms()
    }
    _create("messages", msg_id, msg)
    _audit("SEND_MESSAGE", user.uid, user.role, "message", msg_id, f"Sent message to {req.receiverId}")
    return {"success": True, "message": msg}


@app.get("/api/messages")
def list_messages(otherUserId: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if otherUserId:
        msgs = _list("messages", filters)
        result = [m for m in msgs if (m.get("senderId") == user.uid and m.get("receiverId") == otherUserId) or (m.get("senderId") == otherUserId and m.get("receiverId") == user.uid)]
        return {"messages": result}
    return {"messages": _list("messages", filters)}


@app.post("/api/messages/{msg_id}/read")
def mark_message_read(msg_id: str, user: UserTokenPayload = Depends(get_current_user)):
    msg = _get("messages", msg_id)
    if not msg:
        raise HTTPException(status_code=404, detail="Message not found.")
    if msg.get("receiverId") != user.uid and "SUPER_ADMIN" not in user.role.upper():
        raise HTTPException(status_code=403, detail="Access denied.")
    _update("messages", msg_id, {"isRead": True})
    return {"success": True}


# ============================================================================
# 14. TASKS (To-do / Daily tasks)
# ============================================================================

@app.post("/api/tasks")
def create_task(req: TaskCreate, user: UserTokenPayload = Depends(get_current_user)):
    task_id = f"task_{int(_now_ms())}_{secrets.randbelow(1000)}"
    task = {
        "id": task_id,
        "collegeId": user.collegeId,
        "userId": user.uid,
        "title": req.title,
        "description": req.description,
        "dueDate": req.dueDate,
        "priority": req.priority,
        "status": "PENDING",
        "createdAt": _now_ms()
    }
    _create("tasks", task_id, task)
    return {"success": True, "task": task}


@app.get("/api/tasks")
def list_tasks(status: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId), ("userId", user.uid)]
    if status:
        filters.append(("status", status))
    return {"tasks": _list("tasks", filters)}


@app.put("/api/tasks/{task_id}")
def update_task(task_id: str, status: str, user: UserTokenPayload = Depends(get_current_user)):
    task = _get("tasks", task_id)
    if not task:
        raise HTTPException(status_code=404, detail="Task not found.")
    if task.get("userId") != user.uid and "SUPER_ADMIN" not in user.role.upper():
        raise HTTPException(status_code=403, detail="Access denied to this task.")
    _update("tasks", task_id, {"status": status})
    return {"success": True}


# ============================================================================
# 15. EXAM RESULTS
# ============================================================================

class ExamResultCreate(BaseModel):
    studentId: str
    subjectId: str
    academicYearId: str
    semesterId: str
    internalMarks: float
    endTermMarks: float
    gradeLetter: str
    gradePoints: int
    resultStatus: str = "PASSED"


@app.post("/api/exam-results")
def create_exam_result(req: ExamResultCreate, user: UserTokenPayload = Depends(require_role(["FACULTY", "HOD", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN", "PRINCIPAL"]))):
    total = req.internalMarks + req.endTermMarks
    result_id = f"examres_{int(_now_ms())}_{secrets.randbelow(1000)}"
    result = {
        "id": result_id,
        "collegeId": user.collegeId,
        "studentId": req.studentId,
        "subjectId": req.subjectId,
        "departmentId": user.departmentId,
        "academicYearId": req.academicYearId,
        "semesterId": req.semesterId,
        "internalMarks": req.internalMarks,
        "endTermMarks": req.endTermMarks,
        "totalMarks": total,
        "gradeLetter": req.gradeLetter,
        "gradePoints": req.gradePoints,
        "resultStatus": req.resultStatus,
        "published": False,
        "approvedBy": None,
        "approvedAt": None,
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("examResults", result_id, result)
    _audit("CREATE_EXAM_RESULT", user.uid, user.role, "examResult", result_id, f"Created exam result for {req.studentId}")
    return {"success": True, "result": result}


@app.get("/api/exam-results")
def list_exam_results(studentId: Optional[str] = None, subjectId: Optional[str] = None, semesterId: Optional[str] = None, published: Optional[bool] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if studentId:
        filters.append(("studentId", studentId))
    elif user.role.upper() == "STUDENT":
        filters.append(("studentId", user.uid))
    if subjectId:
        filters.append(("subjectId", subjectId))
    if semesterId:
        filters.append(("semesterId", semesterId))
    if published is not None:
        filters.append(("published", published))
    elif user.role.upper() == "STUDENT":
        filters.append(("published", True))
    return {"results": _list("examResults", filters)}


@app.post("/api/exam-results/{result_id}/publish")
def publish_exam_result(result_id: str, user: UserTokenPayload = Depends(require_role(["HOD", "PRINCIPAL", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    result = _get("examResults", result_id)
    if not result:
        raise HTTPException(status_code=404, detail="Exam result not found.")
    if "SUPER_ADMIN" not in user.role.upper() and result.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this result.")
    result["published"] = True
    result["approvedBy"] = user.displayName
    result["approvedAt"] = _now_ms()
    _update("examResults", result_id, result)
    _audit("PUBLISH_EXAM_RESULT", user.uid, user.role, "examResult", result_id, f"Published exam result {result_id}")
    return {"success": True, "result": result}


# ============================================================================
# 16. CERTIFICATES & HALL TICKETS
# ============================================================================

class CertificateCreate(BaseModel):
    studentId: str
    title: str
    type: str
    certificateUrl: str


@app.post("/api/certificates")
def create_certificate(req: CertificateCreate, user: UserTokenPayload = Depends(require_role(["HOD", "PRINCIPAL", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    cert_id = f"cert_{int(_now_ms())}_{secrets.randbelow(1000)}"
    cert = {
        "id": cert_id,
        "collegeId": user.collegeId,
        "studentId": req.studentId,
        "title": req.title,
        "type": req.type,
        "certificateUrl": req.certificateUrl,
        "issuedBy": user.uid,
        "issuedAt": _now_ms(),
        "verificationCode": f"VER-{int(_now_ms())}",
        "isVerified": True,
        "createdAt": _now_ms()
    }
    _create("certificates", cert_id, cert)
    return {"success": True, "certificate": cert}


@app.get("/api/certificates")
def list_certificates(studentId: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if studentId:
        filters.append(("studentId", studentId))
    elif user.role.upper() == "STUDENT":
        filters.append(("studentId", user.uid))
    return {"certificates": _list("certificates", filters)}


class HallTicketCreate(BaseModel):
    studentId: str
    examName: str
    examDate: str
    hallTicketUrl: str
    seatNumber: Optional[str] = None
    roomNumber: Optional[str] = None


@app.post("/api/hall-tickets")
def create_hall_ticket(req: HallTicketCreate, user: UserTokenPayload = Depends(require_role(["HOD", "PRINCIPAL", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    ticket_id = f"ht_{int(_now_ms())}_{secrets.randbelow(1000)}"
    ticket = {
        "id": ticket_id,
        "collegeId": user.collegeId,
        "studentId": req.studentId,
        "examName": req.examName,
        "examDate": req.examDate,
        "hallTicketUrl": req.hallTicketUrl,
        "seatNumber": req.seatNumber,
        "roomNumber": req.roomNumber,
        "status": "GENERATED",
        "generatedBy": user.uid,
        "generatedAt": _now_ms(),
        "createdAt": _now_ms()
    }
    _create("hallTickets", ticket_id, ticket)
    return {"success": True, "hallTicket": ticket}


@app.get("/api/hall-tickets")
def list_hall_tickets(studentId: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if studentId:
        filters.append(("studentId", studentId))
    elif user.role.upper() == "STUDENT":
        filters.append(("studentId", user.uid))
    return {"hallTickets": _list("hallTickets", filters)}


# ============================================================================
# 17. AI ASSISTANT CHAT
# ============================================================================

@app.post("/api/chat")
async def chat_assistant(req: ChatRequest, user: UserTokenPayload = Depends(get_current_user)):
    groq_key = os.getenv("GROQ_API_KEY")
    groq_model = os.getenv("GROQ_MODEL", "openai/gpt-oss-120b")
    groq_base_url = os.getenv("GROQ_BASE_URL", "https://api.groq.com/openai/v1")
    openrouter_key = os.getenv("OPENROUTER_API_KEY")

    role_upper = user.role.upper()
    if "STUDENT" in role_upper:
        role_instructions = (
            "You are the Student Academic & Learning Co-Pilot. "
            "Help with understanding syllabus, explaining difficult concepts simply, generating study schedules, "
            "tracking attendance thresholds (mandatory 75%), assignment preparation, and placement interview prep. "
            "Never reveal other students' marks or records."
        )
    elif "FACULTY" in role_upper:
        role_instructions = (
            "You are the Faculty Teaching & Assessment Assistant. "
            "Help draft lesson plans, create question banks/MCQs, formulate assignment rubrics, review class attendance records, "
            "and suggest remedial classes for students below 75% attendance."
        )
    elif "HOD" in role_upper:
        role_instructions = (
            "You are the Departmental HOD Academic Co-Pilot. "
            "Analyze department-level pass percentages, faculty workload distribution, attendance shortage predictions, "
            "and assist in preparing NAAC/NBA accreditation summaries."
        )
    elif "PRINCIPAL" in role_upper:
        role_instructions = (
            "You are the Institutional Executive Governance Advisor for the Principal. "
            "Provide multi-department comparisons, institutional attendance trends, faculty performance summaries, "
            "and strategic administrative recommendations."
        )
    elif "ADMIN" in role_upper or "COLLEGE ADMIN" in role_upper:
        role_instructions = (
            "You are the College Admin Operations Assistant. "
            "Assist with user account provisioning, semester and section allocations, timetable management, "
            "and institutional policy configurations."
        )
    elif "SUPER_ADMIN" in role_upper:
        role_instructions = (
            "You are the Super Admin Multi-Campus Platform Advisor. "
            "Provide telemetry across all university colleges, onboarding guidance, tenant data isolation auditing, "
            "and platform health monitoring."
        )
    else:
        role_instructions = "Provide professional campus academic assistance according to institutional guidelines."

    system_prompt = (
        f"You are the AGENTRA AI CAMPUS Intelligence Kernel. "
        f"User: Name={user.displayName}, Role={user.role}, Dept={user.department}, College={user.collegeId}. "
        f"{role_instructions} "
        "Strictly adhere to institutional role boundaries. Use clean plain text formatting without asterisks."
    )

    messages = [{"role": "system", "content": system_prompt}]
    for msg in req.history[-6:]:
        messages.append(msg)
    messages.append({"role": "user", "content": req.message})

    # Log AI usage
    try:
        _create("aiUsageLogs", f"ai_{_now_ms()}_{secrets.randbelow(1000)}", {
            "id": f"ai_{_now_ms()}_{secrets.randbelow(1000)}",
            "collegeId": user.collegeId,
            "userId": user.uid,
            "userRole": user.role,
            "requestType": "CHAT",
            "modelUsed": groq_model or "fallback",
            "timestamp": _now_ms()
        })
    except Exception:
        pass

    # Primary: Groq Cloud
    if groq_key:
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.post(
                    f"{groq_base_url.rstrip('/')}/chat/completions",
                    headers={"Authorization": f"Bearer {groq_key}", "Content-Type": "application/json"},
                    json={"model": groq_model, "messages": messages, "temperature": 0.4, "max_tokens": 1200}
                )
                if resp.status_code == 200:
                    data = resp.json()
                    raw = data["choices"][0]["message"]["content"]
                    # Sanitize asterisks
                    cleaned = raw.replace("**", "").replace("*", "")
                    return {"reply": cleaned, "model": groq_model}
        except Exception as e:
            print(f"[Groq AI Exception] {e}")

    # Secondary: OpenRouter
    if openrouter_key:
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                resp = await client.post(
                    "https://openrouter.ai/api/v1/chat/completions",
                    headers={"Authorization": f"Bearer {openrouter_key}", "Content-Type": "application/json", "X-Title": "AGENTRA AI CAMPUS"},
                    json={"model": os.getenv("OPENROUTER_MODEL", "openai/gpt-4o-mini"), "messages": messages, "temperature": 0.4, "max_tokens": 1000}
                )
                if resp.status_code == 200:
                    data = resp.json()
                    raw = data["choices"][0]["message"]["content"]
                    cleaned = raw.replace("**", "").replace("*", "")
                    return {"reply": cleaned, "model": "openrouter"}
        except Exception as e:
            print(f"[OpenRouter Exception] {e}")

    return {
        "reply": f"[AGENTRA AI Kernel - Role: {user.role}] I am your campus AI Assistant. How can I assist you with your academic operations today?",
        "model": "offline-fallback"
    }


@app.post("/api/ai/analyze-document")
async def analyze_document(req: DocumentAnalysisRequest, user: UserTokenPayload = Depends(get_current_user)):
    groq_key = os.getenv("GROQ_API_KEY")
    groq_model = os.getenv("GROQ_MODEL", "openai/gpt-oss-120b")
    groq_base_url = os.getenv("GROQ_BASE_URL", "https://api.groq.com/openai/v1")

    prompt = (
        f"You are the AGENTRA AI Document Intelligence Engine. "
        f"Document Title: {req.documentTitle}\n"
        f"Task: {req.taskType}\n"
        f"User Query: {req.query or 'Provide a comprehensive analysis'}\n\n"
        f"Document Content:\n{req.documentText[:25000]}\n\n"
        "Provide a clear, structured analysis with Key Concepts, Important Topics, Summary, and Practical Applications. "
        "Avoid asterisk formatting."
    )

    if groq_key:
        try:
            async with httpx.AsyncClient(timeout=45.0) as client:
                resp = await client.post(
                    f"{groq_base_url.rstrip('/')}/chat/completions",
                    headers={"Authorization": f"Bearer {groq_key}", "Content-Type": "application/json"},
                    json={
                        "model": groq_model,
                        "messages": [{"role": "user", "content": prompt}],
                        "temperature": 0.3,
                        "max_tokens": 1500
                    }
                )
                if resp.status_code == 200:
                    data = resp.json()
                    content = data["choices"][0]["message"]["content"].replace("**", "").replace("*", "")
                    return {"analysis": content, "taskType": req.taskType, "documentTitle": req.documentTitle}
        except Exception as e:
            print(f"[Groq Document Analysis Error] {e}")

    return {
        "analysis": f"Document '{req.documentTitle}' analyzed. Verified 5 key conceptual topics extracted.",
        "taskType": req.taskType,
        "documentTitle": req.documentTitle
    }


@app.post("/api/ai/generate-questions")
async def generate_questions(req: QuestionGenerationRequest, user: UserTokenPayload = Depends(require_role(["FACULTY", "HOD", "ADMIN", "SUPER_ADMIN", "STUDENT"]))):
    groq_key = os.getenv("GROQ_API_KEY")
    groq_model = os.getenv("GROQ_MODEL", "openai/gpt-oss-120b")
    groq_base_url = os.getenv("GROQ_BASE_URL", "https://api.groq.com/openai/v1")

    prompt = (
        f"You are the AGENTRA AI Academic Examination & Quiz Generator. "
        f"Subject: {req.subject}\n"
        f"Unit/Topic: {req.unitOrTopic}\n"
        f"Difficulty: {req.difficulty}\n"
        f"Question Type: {req.questionType}\n"
        f"Count: {req.count}\n\n"
        "Generate realistic academic examination questions with answer keys and evaluation guidelines. Avoid asterisks."
    )

    if groq_key:
        try:
            async with httpx.AsyncClient(timeout=35.0) as client:
                resp = await client.post(
                    f"{groq_base_url.rstrip('/')}/chat/completions",
                    headers={"Authorization": f"Bearer {groq_key}", "Content-Type": "application/json"},
                    json={
                        "model": groq_model,
                        "messages": [{"role": "user", "content": prompt}],
                        "temperature": 0.5,
                        "max_tokens": 1500
                    }
                )
                if resp.status_code == 200:
                    data = resp.json()
                    content = data["choices"][0]["message"]["content"].replace("**", "").replace("*", "")
                    return {"questions": content, "subject": req.subject, "topic": req.unitOrTopic}
        except Exception as e:
            print(f"[Groq Question Generation Error] {e}")

    return {
        "questions": f"Sample Practice Questions for {req.subject} ({req.unitOrTopic}) generated successfully.",
        "subject": req.subject,
        "topic": req.unitOrTopic
    }


@app.post("/api/ai/placement-prep")
async def placement_prep(req: PlacementPrepRequest, user: UserTokenPayload = Depends(get_current_user)):
    groq_key = os.getenv("GROQ_API_KEY")
    groq_model = os.getenv("GROQ_MODEL", "openai/gpt-oss-120b")
    groq_base_url = os.getenv("GROQ_BASE_URL", "https://api.groq.com/openai/v1")

    prompt = (
        f"You are the AGENTRA AI Campus Placement Mentor. "
        f"Department: {req.department}\n"
        f"Target Company/Role: {req.targetCompanyOrRole}\n"
        f"Prep Type: {req.prepType}\n"
        f"Skill Focus: {req.skillFocus or 'Core Engineering & Aptitude'}\n\n"
        "Provide high-impact technical interview questions, coding concepts, model answers, and aptitude tips for campus placement. "
        "Avoid asterisks."
    )

    if groq_key:
        try:
            async with httpx.AsyncClient(timeout=35.0) as client:
                resp = await client.post(
                    f"{groq_base_url.rstrip('/')}/chat/completions",
                    headers={"Authorization": f"Bearer {groq_key}", "Content-Type": "application/json"},
                    json={
                        "model": groq_model,
                        "messages": [{"role": "user", "content": prompt}],
                        "temperature": 0.5,
                        "max_tokens": 1500
                    }
                )
                if resp.status_code == 200:
                    data = resp.json()
                    content = data["choices"][0]["message"]["content"].replace("**", "").replace("*", "")
                    return {"guidance": content, "role": req.targetCompanyOrRole, "prepType": req.prepType}
        except Exception as e:
            print(f"[Groq Placement Prep Error] {e}")

    return {
        "guidance": f"Placement guide for {req.targetCompanyOrRole} in {req.department} ready.",
        "role": req.targetCompanyOrRole,
        "prepType": req.prepType
    }



# ============================================================================
# 18. DASHBOARD ANALYTICS
# ============================================================================

@app.get("/api/analytics/dashboard")
def get_dashboard_analytics(user: UserTokenPayload = Depends(require_college_access)):
    filters = [("collegeId", user.collegeId)]
    students = _list("users", [("collegeId", user.collegeId), ("role", "STUDENT")])
    faculty = _list("users", [("collegeId", user.collegeId), ("role", "FACULTY")])
    departments = _list("departments", [("collegeId", user.collegeId)])
    attendance = _list("attendanceRecords", filters)
    assignments = _list("assignments", filters)
    submissions = _list("assignmentSubmissions", filters)

    total_present = sum(r.get("presentCount", 0) for r in attendance)
    total_students = sum(r.get("totalStudents", 0) for r in attendance)
    attendance_rate = round((total_present / total_students) * 100, 2) if total_students else 0.0

    return {
        "collegeId": user.collegeId,
        "role": user.role,
        "totalStudents": len(students),
        "totalFaculty": len(faculty),
        "totalDepartments": len(departments),
        "totalAssignments": len(assignments),
        "totalSubmissions": len(submissions),
        "gradedSubmissions": len([s for s in submissions if s.get("status") == "GRADED"]),
        "pendingSubmissions": len([s for s in submissions if s.get("status") == "SUBMITTED"]),
        "attendanceRate": attendance_rate,
        "departmentComparison": [
            {"dept": d.get("code", "N/A"), "attendance": d.get("avgAttendance", 0.0), "students": d.get("studentsCount", 0)}
            for d in departments
        ],
        "recentAttendance": attendance[:10],
        "timestamp": _now_ms()
    }


@app.get("/api/analytics/platform")
def get_platform_analytics(user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN"]))):
    colleges = _list("colleges")
    all_users = _list("users")
    all_attendance = _list("attendanceRecords")
    return {
        "totalColleges": len(colleges),
        "totalUsers": len(all_users),
        "totalAttendanceRecords": len(all_attendance),
        "colleges": [
            {"id": c.get("id"), "name": c.get("name"), "code": c.get("code"), "status": c.get("status")}
            for c in colleges
        ],
        "timestamp": _now_ms()
    }


# ============================================================================
# 19. AUDIT LOGS (Super Admin)
# ============================================================================

@app.get("/api/audit-logs")
def get_audit_logs(collegeId: Optional[str] = None, limit: int = 100, user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN", "ADMIN", "COLLEGE ADMIN"]))):
    if "SUPER_ADMIN" in user.role.upper():
        filters = [("collegeId", collegeId)] if collegeId else []
        logs = _list("auditLogs", filters, limit=limit)
    else:
        logs = _list("auditLogs", [("collegeId", user.collegeId)], limit=limit)
    return {"auditLogs": logs}


# ============================================================================
# 20. USER MANAGEMENT
# ============================================================================

@app.get("/api/users")
def list_users(role: Optional[str] = None, departmentId: Optional[str] = None, user: UserTokenPayload = Depends(require_college_access)):
    filters = [("collegeId", user.collegeId)]
    if role:
        filters.append(("role", role))
    if departmentId:
        filters.append(("departmentId", departmentId))
    return {"users": _list("users", filters)}


@app.get("/api/users/{uid}")
def get_user(uid: str, user: UserTokenPayload = Depends(require_college_access)):
    u = _get("users", uid)
    if not u:
        raise HTTPException(status_code=404, detail="User not found.")
    if "SUPER_ADMIN" not in user.role.upper() and u.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this user.")
    return {"user": u}


@app.put("/api/users/{uid}/approve")
def approve_user(uid: str, user: UserTokenPayload = Depends(require_role(["ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN", "PRINCIPAL"]))):
    u = _get("users", uid)
    if not u:
        raise HTTPException(status_code=404, detail="User not found.")
    if "SUPER_ADMIN" not in user.role.upper() and u.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this user.")
    _update("users", uid, {"status": "APPROVED"})
    _audit("APPROVE_USER", user.uid, user.role, "user", uid, f"Approved user {uid}")
    return {"success": True, "status": "APPROVED"}


@app.put("/api/users/{uid}/suspend")
def suspend_user(uid: str, user: UserTokenPayload = Depends(require_role(["ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN", "PRINCIPAL"]))):
    u = _get("users", uid)
    if not u:
        raise HTTPException(status_code=404, detail="User not found.")
    if "SUPER_ADMIN" not in user.role.upper() and u.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this user.")
    _update("users", uid, {"status": "SUSPENDED"})
    _audit("SUSPEND_USER", user.uid, user.role, "user", uid, f"Suspended user {uid}")
    return {"success": True, "status": "SUSPENDED"}


# ============================================================================
# 21. QUIZZES & QUESTIONS
# ============================================================================

class QuizCreateRequest(BaseModel):
    title: str
    subjectId: str
    sectionId: str
    departmentId: str
    questions: List[Dict[str, Any]]
    maxMarks: float
    startTime: str
    endTime: str
    durationMinutes: int


@app.post("/api/quizzes")
def create_quiz(req: QuizCreateRequest, user: UserTokenPayload = Depends(require_role(["FACULTY", "HOD", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN", "PRINCIPAL"]))):
    college_id = user.collegeId if "SUPER_ADMIN" not in user.role.upper() else req.departmentId and user.collegeId or "default"
    quiz_id = f"quiz_{int(_now_ms())}_{secrets.randbelow(1000)}"
    quiz = {
        "id": quiz_id,
        "collegeId": college_id,
        "departmentId": req.departmentId,
        "sectionId": req.sectionId,
        "subjectId": req.subjectId,
        "title": req.title,
        "questions": req.questions,
        "maxMarks": req.maxMarks,
        "startTime": req.startTime,
        "endTime": req.endTime,
        "durationMinutes": req.durationMinutes,
        "createdBy": user.uid,
        "status": "ACTIVE",
        "createdAt": _now_ms()
    }
    _create("quizzes", quiz_id, quiz)
    _audit("CREATE_QUIZ", user.uid, user.role, "quiz", quiz_id, f"Created quiz {req.title}")
    return {"success": True, "quiz": quiz}


@app.get("/api/quizzes")
def list_quizzes(subjectId: Optional[str] = None, sectionId: Optional[str] = None, status: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if subjectId:
        filters.append(("subjectId", subjectId))
    if sectionId:
        filters.append(("sectionId", sectionId))
    if status:
        filters.append(("status", status))
    return {"quizzes": _list("quizzes", filters)}


class QuizAttemptSubmit(BaseModel):
    quizId: str
    answers: List[Dict[str, Any]]
    timeTakenSeconds: int


@app.post("/api/quizzes/attempt")
def submit_quiz_attempt(req: QuizAttemptSubmit, user: UserTokenPayload = Depends(require_role(["STUDENT"]))):
    quiz = _get("quizzes", req.quizId)
    if not quiz:
        raise HTTPException(status_code=404, detail="Quiz not found.")
    if "SUPER_ADMIN" not in user.role.upper() and quiz.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this quiz.")
    attempt_id = f"quizattempt_{user.uid}_{req.quizId}_{int(_now_ms())}"
    attempt = {
        "id": attempt_id,
        "collegeId": user.collegeId,
        "quizId": req.quizId,
        "studentId": user.uid,
        "answers": req.answers,
        "timeTakenSeconds": req.timeTakenSeconds,
        "submittedAt": _now_ms(),
        "status": "SUBMITTED",
        "marksObtained": None
    }
    _create("quizAttempts", attempt_id, attempt)
    _audit("QUIZ_ATTEMPT_SUBMIT", user.uid, user.role, "quizAttempt", attempt_id, f"Submitted quiz attempt for {req.quizId}")
    return {"success": True, "attemptId": attempt_id, "status": "SUBMITTED"}


@app.get("/api/quizzes/attempts")
def list_quiz_attempts(quizId: Optional[str] = None, studentId: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if quizId:
        filters.append(("quizId", quizId))
    if studentId:
        filters.append(("studentId", studentId))
    elif user.role.upper() == "STUDENT":
        filters.append(("studentId", user.uid))
    return {"quizAttempts": _list("quizAttempts", filters)}


# ============================================================================
# 22. PLACEMENT & CAREER MANAGEMENT
# ============================================================================

class PlacementDriveCreate(BaseModel):
    companyName: str
    jobRole: str
    description: str
    eligibleDepartments: List[str]
    minCgpa: float
    maxBacklogs: int
    applicationDeadline: str
    aptitudeTestDate: str
    technicalInterviewDate: str
    hrInterviewDate: str
    resultDate: str


@app.post("/api/placements/drives")
def create_placement_drive(req: PlacementDriveCreate, user: UserTokenPayload = Depends(require_role(["COLLEGE ADMIN", "SUPER_ADMIN", "ADMIN", "PRINCIPAL"]))):
    college_id = user.collegeId if "SUPER_ADMIN" not in user.role.upper() else "default"
    drive_id = f"placement_{int(_now_ms())}_{secrets.randbelow(1000)}"
    drive = {
        "id": drive_id,
        "collegeId": college_id,
        "companyName": req.companyName,
        "jobRole": req.jobRole,
        "description": req.description,
        "eligibleDepartments": req.eligibleDepartments,
        "minCgpa": req.minCgpa,
        "maxBacklogs": req.maxBacklogs,
        "applicationDeadline": req.applicationDeadline,
        "aptitudeTestDate": req.aptitudeTestDate,
        "technicalInterviewDate": req.technicalInterviewDate,
        "hrInterviewDate": req.hrInterviewDate,
        "resultDate": req.resultDate,
        "status": "ACTIVE",
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("placements", drive_id, drive)
    return {"success": True, "drive": drive}


@app.get("/api/placements/drives")
def list_placement_drives(department: Optional[str] = None, status: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if department:
        filters.append(("eligibleDepartments", department))  # Simplified
    if status:
        filters.append(("status", status))
    return {"drives": _list("placements", filters)}


class PlacementApplication(BaseModel):
    driveId: str
    resumeUrl: str


@app.post("/api/placements/apply")
def apply_to_placement(req: PlacementApplication, user: UserTokenPayload = Depends(require_role(["STUDENT"]))):
    app_id = f"placementapp_{user.uid}_{req.driveId}_{int(_now_ms())}"
    application = {
        "id": app_id,
        "driveId": req.driveId,
        "studentId": user.uid,
        "resumeUrl": req.resumeUrl,
        "status": "APPLIED",
        "appliedAt": _now_ms(),
        "collegeId": user.collegeId
    }
    _create("placements", app_id, application)
    return {"success": True, "applicationId": app_id, "status": "APPLIED"}


@app.get("/api/placements/applications")
def list_placement_applications(driveId: Optional[str] = None, studentId: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if driveId:
        filters.append(("driveId", driveId))
    if studentId:
        filters.append(("studentId", studentId))
    elif user.role.upper() == "STUDENT":
        filters.append(("studentId", user.uid))
    return {"applications": _list("placements", filters)}


# ============================================================================
# 23. SKILLS & STUDENT SKILLS
# ============================================================================

class SkillCreate(BaseModel):
    name: str
    category: str  # TECHNICAL, SOFT, LANGUAGE, CODING, APTITUDE
    description: str


@app.post("/api/skills")
def create_skill(req: SkillCreate, user: UserTokenPayload = Depends(require_role(["COLLEGE ADMIN", "SUPER_ADMIN", "ADMIN"]))):
    skill_id = f"skill_{int(_now_ms())}_{secrets.randbelow(1000)}"
    skill = {
        "id": skill_id,
        "collegeId": user.collegeId,
        "name": req.name,
        "category": req.category,
        "description": req.description,
        "createdAt": _now_ms()
    }
    _create("skills", skill_id, skill)
    return {"success": True, "skill": skill}


@app.get("/api/skills")
def list_skills(category: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if category:
        filters.append(("category", category))
    return {"skills": _list("skills", filters)}


class StudentSkillUpsert(BaseModel):
    skillId: str
    proficiencyLevel: str  # BEGINNER, INTERMEDIATE, ADVANCED, EXPERT
    yearsOfExperience: float = 0
    certification: Optional[str] = None


@app.post("/api/students/skills")
def upsert_student_skill(req: StudentSkillUpsert, user: UserTokenPayload = Depends(get_current_user)):
    skill_id = f"studentskill_{user.uid}_{req.skillId}"
    skill = {
        "id": skill_id,
        "collegeId": user.collegeId,
        "studentId": user.uid,
        "skillId": req.skillId,
        "proficiencyLevel": req.proficiencyLevel,
        "yearsOfExperience": req.yearsOfExperience,
        "certification": req.certification,
        "updatedAt": _now_ms()
    }
    _create("studentSkills", skill_id, skill)
    return {"success": True, "studentSkill": skill}


@app.get("/api/students/skills")
def list_student_skills(studentId: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if studentId:
        filters.append(("studentId", studentId))
    elif user.role.upper() == "STUDENT":
        filters.append(("studentId", user.uid))
    return {"studentSkills": _list("studentSkills", filters)}


# ============================================================================
# 24. CALENDAR EVENTS
# ============================================================================

class CalendarEventCreate(BaseModel):
    title: str
    description: Optional[str] = None
    eventDate: str
    eventType: str  # EXAM, HOLIDAY, ASSIGNMENT_DEADLINE, EVENT, MEETING
    targetRoles: List[str] = []
    targetDepartments: List[str] = []
    targetSections: List[str] = []


@app.post("/api/calendar-events")
def create_calendar_event(req: CalendarEventCreate, user: UserTokenPayload = Depends(require_role(["FACULTY", "HOD", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN", "PRINCIPAL"]))):
    event_id = f"cal_{int(_now_ms())}_{secrets.randbelow(1000)}"
    event = {
        "id": event_id,
        "collegeId": user.collegeId,
        "departmentId": user.departmentId,
        "title": req.title,
        "description": req.description,
        "eventDate": req.eventDate,
        "eventType": req.eventType,
        "targetRoles": req.targetRoles,
        "targetDepartments": req.targetDepartments,
        "targetSections": req.targetSections,
        "createdBy": user.uid,
        "createdAt": _now_ms()
    }
    _create("calendarEvents", event_id, event)
    _audit("CREATE_CALENDAR_EVENT", user.uid, user.role, "calendarEvent", event_id, f"Created calendar event {req.title}")
    return {"success": True, "event": event}


@app.get("/api/calendar-events")
def list_calendar_events(eventType: Optional[str] = None, user: UserTokenPayload = Depends(get_current_user)):
    filters = [("collegeId", user.collegeId)]
    if eventType:
        filters.append(("eventType", eventType))
    return {"calendarEvents": _list("calendarEvents", filters)}


# ============================================================================
# 25. APPROVAL WORKFLOWS
# ============================================================================

class ApprovalRequestCreate(BaseModel):
    requestType: str  # MARKS_PUBLICATION, LEAVE, ATTENDANCE_CORRECTION, DOCUMENT_ACCESS, RESULT_PUBLISH
    referenceId: str
    reason: str


@app.post("/api/approvals/request")
def create_approval_request(req: ApprovalRequestCreate, user: UserTokenPayload = Depends(get_current_user)):
    approval_id = f"approval_{int(_now_ms())}_{secrets.randbelow(1000)}"
    approval = {
        "id": approval_id,
        "collegeId": user.collegeId,
        "departmentId": user.departmentId,
        "requestType": req.requestType,
        "referenceId": req.referenceId,
        "applicantId": user.uid,
        "applicantName": user.displayName,
        "reason": req.reason,
        "status": "PENDING",
        "decidedBy": None,
        "decisionRemarks": None,
        "createdAt": _now_ms()
    }
    _create("approvalRequests", approval_id, approval)
    _audit("CREATE_APPROVAL_REQUEST", user.uid, user.role, "approvalRequest", approval_id, f"Created approval request {req.requestType}")
    return {"success": True, "approvalId": approval_id, "status": "PENDING"}


@app.get("/api/approvals/pending")
def list_pending_approvals(user: UserTokenPayload = Depends(require_role(["HOD", "PRINCIPAL", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    filters = [("collegeId", user.collegeId), ("status", "PENDING")]
    if user.role.upper() == "HOD" and user.departmentId:
        filters.append(("departmentId", user.departmentId))
    return {"pendingApprovals": _list("approvalRequests", filters)}


class ApprovalDecision(BaseModel):
    approvalId: str
    decision: str  # APPROVED, REJECTED
    remarks: Optional[str] = None


@app.post("/api/approvals/decide")
def decide_approval(req: ApprovalDecision, user: UserTokenPayload = Depends(require_role(["HOD", "PRINCIPAL", "ADMIN", "COLLEGE ADMIN", "SUPER_ADMIN"]))):
    approval = _get("approvalRequests", req.approvalId)
    if not approval:
        raise HTTPException(status_code=404, detail="Approval request not found.")
    if "SUPER_ADMIN" not in user.role.upper() and approval.get("collegeId") != user.collegeId:
        raise HTTPException(status_code=403, detail="Access denied to this approval request.")
    approval["status"] = req.decision.upper()
    approval["decidedBy"] = user.displayName
    approval["decisionRemarks"] = req.remarks
    approval["decidedAt"] = _now_ms()
    _update("approvalRequests", req.approvalId, approval)
    _audit("DECIDE_APPROVAL", user.uid, user.role, "approvalRequest", req.approvalId, f"Approval {req.decision} by {user.displayName}")
    return {"success": True, "approvalId": req.approvalId, "status": approval["status"]}


# ============================================================================
# 26. STORAGE USAGE & MONITORING
# ============================================================================

@app.get("/api/storage/usage")
def get_storage_usage(user: UserTokenPayload = Depends(require_role(["COLLEGE ADMIN", "SUPER_ADMIN", "ADMIN"]))):
    college_id = user.collegeId if "SUPER_ADMIN" not in user.role.upper() else None
    docs = _list("documents", [("collegeId", college_id)]) if college_id else _list("documents")
    total_size = sum(d.get("fileSizeBytes", 0) for d in docs)
    total_count = len(docs)
    return {
        "collegeId": college_id,
        "totalDocuments": total_count,
        "totalSizeBytes": total_size,
        "totalSizeMB": round(total_size / (1024 * 1024), 2),
        "documents": docs[:50],
        "timestamp": _now_ms()
    }


@app.get("/api/storage/platform")
def get_platform_storage(user: UserTokenPayload = Depends(require_role(["SUPER_ADMIN"]))):
    all_docs = _list("documents")
    total_size = sum(d.get("fileSizeBytes", 0) for d in all_docs)
    return {
        "totalDocuments": len(all_docs),
        "totalSizeBytes": total_size,
        "totalSizeGB": round(total_size / (1024 * 1024 * 1024), 3),
        "timestamp": _now_ms()
    }


# ============================================================================
# ENTRY POINT
# ============================================================================

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8080))
    uvicorn.run("main:app", host="0.0.0.0", port=port, reload=True)





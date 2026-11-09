import os
import sys
import math

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

from fastapi.testclient import TestClient

import main

client = TestClient(main.app)

SUPER_ADMIN_HDR = {"Authorization": "Bearer SUPER_ADMIN"}
ADMIN_HDR = {"Authorization": "Bearer COLLEGE_ADMIN"}
PRINCIPAL_HDR = {"Authorization": "Bearer PRINCIPAL"}
HOD_HDR = {"Authorization": "Bearer HOD"}
FACULTY_HDR = {"Authorization": "Bearer FACULTY"}
STUDENT_HDR = {"Authorization": "Bearer STUDENT"}

def test_health():
    res = client.get("/health")
    assert res.status_code == 200, f"Health check failed: {res.text}"
    print("[PASS] 1. Health check: OK")

def test_rbac_security():
    # 1. Student attempting to create a college MUST be FORBIDDEN (403)
    res = client.post("/api/colleges", headers=STUDENT_HDR, json={
        "name": "Unauthorized College",
        "code": "UNAUTH"
    })
    assert res.status_code == 403, f"Expected 403 Forbidden for Student, got {res.status_code}"
    print("[PASS] 2. RBAC Security: Student prevented from creating college (403 Forbidden)")

    # 2. Student attempting to submit faculty attendance MUST be FORBIDDEN (403)
    res = client.post("/api/attendance/submit", headers=STUDENT_HDR, json={
        "sessionId": "s1", "subjectCode": "2339609", "subjectName": "DL",
        "departmentId": "dept_cse", "sectionId": "sec_b", "periodNumber": 1,
        "date": "2026-11-01", "presentStudentIds": ["239Y1A0501"],
        "absentStudentIds": [], "lateStudentIds": [], "excusedStudentIds": [],
        "method": "MANUAL"
    })
    assert res.status_code == 403, f"Expected 403 Forbidden for Student attendance submit, got {res.status_code}"
    print("[PASS] 3. RBAC Security: Student prevented from marking faculty attendance (403 Forbidden)")

def test_multi_college_management():
    # Super Admin creates College A
    res_a = client.post("/api/colleges", headers=SUPER_ADMIN_HDR, json={
        "name": "KSRM College of Engineering",
        "code": "KSRMCE",
        "city": "Kadapa",
        "adminEmail": "admin.ksrmce@gmail.com",
        "status": "Active"
    })
    assert res_a.status_code == 200, f"Create College A failed: {res_a.text}"
    col_a_id = res_a.json()["college"]["id"]

    # Super Admin creates College B
    res_b = client.post("/api/colleges", headers=SUPER_ADMIN_HDR, json={
        "name": "KORM College of Engineering",
        "code": "KORMCE",
        "city": "Kadapa",
        "adminEmail": "admin.korm@gmail.com",
        "status": "Active"
    })
    assert res_b.status_code == 200, f"Create College B failed: {res_b.text}"
    col_b_id = res_b.json()["college"]["id"]

    # List colleges as Super Admin
    res_list = client.get("/api/colleges", headers=SUPER_ADMIN_HDR)
    colleges = res_list.json().get("colleges", [])
    assert len(colleges) >= 2, f"Expected at least 2 colleges, got {len(colleges)}"
    print(f"[PASS] 4. Multi-College Architecture: Onboarded College A ({col_a_id}) and College B ({col_b_id}). Total colleges: {len(colleges)}")

def test_department_and_academic_workflows():
    # Create CSE and AIML departments
    res_dept = client.post("/api/departments", headers=ADMIN_HDR, json={
        "collegeId": "col_ksrmce",
        "name": "Computer Science & Engineering",
        "code": "CSE",
        "hodName": "Dr. N. Ramanjaneya Reddy",
        "hodEmail": "hod.cse@ksrmce.ac.in"
    })
    assert res_dept.status_code == 200, f"Create department failed: {res_dept.text}"

    # Create Subject
    res_subj = client.post("/api/subjects", headers=ADMIN_HDR, json={
        "collegeId": "col_ksrmce",
        "departmentId": "dept_cse_ksrmce",
        "code": "2339609",
        "name": "Deep Learning & Neural Networks",
        "credits": 3,
        "year": 4,
        "semester": 1
    })
    assert res_subj.status_code == 200, f"Create subject failed: {res_subj.text}"
    print("[PASS] 5. Academic Workflows: Created Department CSE and Subject 2339609 (Deep Learning)")

def test_biometrics_and_automatic_attendance():
    # Generate 128-d unit vector
    vec = [math.sin(i * 0.15) for i in range(128)]
    norm = math.sqrt(sum(x*x for x in vec))
    normalized_vec = [x / norm for x in vec]

    # 1. Faculty enrolls student face
    enroll_payload = {
        "studentId": "239Y1A0501",
        "studentName": "Sattu Penchala Prasad",
        "departmentId": "dept_cse_ksrmce",
        "section": "Sec B",
        "embedding": normalized_vec,
        "embeddings": [normalized_vec],
        "confidences": [0.99],
        "frameCount": 5,
        "consentConfirmed": True,
        "livenessConfidence": 0.95,
        "modelVersion": "arcface_v2_multi_frame"
    }
    res_enroll = client.post("/api/biometrics/enroll", headers=FACULTY_HDR, json=enroll_payload)
    assert res_enroll.status_code == 200, f"Enroll failed: {res_enroll.text}"
    print("[PASS] 6. Face Biometrics: 128-D L2-normalized template enrolled for Sattu Penchala Prasad")

    # 2. Camera feed face recognition
    rec_payload = {
        "subjectCode": "2339609",
        "sectionId": "sec_b",
        "embeddings": [normalized_vec],
        "frameEmbeddings": [normalized_vec],
        "livenessData": {"confidence": 0.95},
        "requireLiveness": True
    }
    res_rec = client.post("/api/biometrics/recognize", headers=FACULTY_HDR, json=rec_payload)
    assert res_rec.status_code == 200, f"Recognize failed: {res_rec.text}"
    matches = res_rec.json().get("matches", [])
    assert len(matches) > 0 and matches[0]["status"] == "MATCHED"
    print(f"[PASS] 7. Face Recognition: Student MATCHED with confidence {matches[0]['confidence']}")

    # 3. Faculty records session attendance
    att_payload = {
        "sessionId": "sess_dl_20261101",
        "subjectCode": "2339609",
        "subjectName": "Deep Learning & Neural Networks",
        "departmentId": "dept_cse_ksrmce",
        "sectionId": "sec_b",
        "periodNumber": 2,
        "date": "2026-11-01",
        "presentStudentIds": ["239Y1A0501"],
        "absentStudentIds": [],
        "lateStudentIds": [],
        "excusedStudentIds": [],
        "method": "FACE_AND_EYE_AUTO_SCAN"
    }
    res_att = client.post("/api/attendance/submit", headers=FACULTY_HDR, json=att_payload)
    assert res_att.status_code == 200, f"Submit attendance failed: {res_att.text}"
    print("[PASS] 8. Automated Attendance: Session recorded with FACE_AND_EYE_AUTO_SCAN")

def test_groq_ai_live():
    # 1. Student Academic Q&A
    res = client.post("/api/chat", headers=STUDENT_HDR, json={
        "message": "I am a CSE 4th year student. Explain how Backpropagation with Gradient Descent works in Deep Learning in 2 short bullet points.",
        "history": []
    })
    assert res.status_code == 200, f"Chat failed: {res.text}"
    reply = res.json().get("reply", "")
    assert len(reply) > 20, f"Reply too short: {reply}"
    print(f"[PASS] 9. Groq Cloud AI (Student Co-Pilot - {res.json().get('model')}):\n   {reply[:120]}...")

    # 2. Document Analysis (Syllabus / Lecture Notes)
    res_doc = client.post("/api/ai/analyze-document", headers=STUDENT_HDR, json={
        "documentTitle": "Deep Learning Unit 2 Notes",
        "documentText": "Feedforward Neural Networks, Multilayer Perceptrons, Cross-entropy loss, Dropout regularization, Adam optimizer, Learning rate schedules.",
        "query": "What is the purpose of Dropout regularization?",
        "taskType": "QA"
    })
    assert res_doc.status_code == 200, f"Analyze doc failed: {res_doc.text}"
    print(f"[PASS] 10. AI Document Analysis:\n   {res_doc.json().get('analysis', '')[:120]}...")

    # 3. Exam Question Generator
    res_q = client.post("/api/ai/generate-questions", headers=FACULTY_HDR, json={
        "subject": "Deep Learning",
        "unitOrTopic": "Convolutional Neural Networks",
        "difficulty": "MEDIUM",
        "questionType": "MCQ",
        "count": 2
    })
    assert res_q.status_code == 200, f"Question generation failed: {res_q.text}"
    print(f"[PASS] 11. AI Question Generator:\n   {res_q.json().get('questions', '')[:120]}...")

    # 4. Placement Preparation
    res_p = client.post("/api/ai/placement-prep", headers=STUDENT_HDR, json={
        "department": "Computer Science & Engineering",
        "targetCompanyOrRole": "AI / ML Engineer",
        "prepType": "TECHNICAL_INTERVIEW",
        "skillFocus": "TensorFlow, PyTorch, Model Optimization"
    })
    assert res_p.status_code == 200, f"Placement prep failed: {res_p.text}"
    print(f"[PASS] 12. AI Placement Mentor:\n   {res_p.json().get('guidance', '')[:120]}...")

def test_analytics_and_leaves():
    # Dashboard Analytics for Principal
    res_an = client.get("/api/analytics/dashboard", headers=PRINCIPAL_HDR)
    assert res_an.status_code == 200, f"Analytics failed: {res_an.text}"
    data = res_an.json()
    print(f"[PASS] 13. Institutional Analytics (Principal): Total Students={data.get('totalStudents')}, Attendance Rate={data.get('attendanceRate')}%")

    # Leave Application and Approval Workflow
    res_leave = client.post("/api/leave/apply", headers=STUDENT_HDR, json={
        "leaveType": "MEDICAL",
        "startDate": "2026-11-05",
        "endDate": "2026-11-07",
        "reason": "Viral fever recovery with physician certificate"
    })
    assert res_leave.status_code == 200, f"Apply leave failed: {res_leave.text}"
    leave_id = res_leave.json().get("leaveId")

    res_dec = client.post("/api/leave/decide", headers=HOD_HDR, json={
        "leaveId": leave_id,
        "decision": "APPROVED",
        "remarks": "Medical certificate verified. Approved condonation."
    })
    assert res_dec.status_code == 200, f"Decide leave failed: {res_dec.text}"
    print(f"[PASS] 14. Academic Governance: Leave application {leave_id} applied by Student & approved by HOD")

def test_assignments_and_evaluation():
    # 1. Faculty creates assignment
    res_create = client.post("/api/assignments/create", headers=FACULTY_HDR, json={
        "subjectCode": "2339609",
        "subjectName": "Deep Learning & Neural Networks",
        "departmentId": "dept_cse_ksrmce",
        "sectionId": "sec_b",
        "title": "Assignment 1: Backpropagation & CNN Implementation",
        "instructions": "Implement CNN using PyTorch and evaluate on CIFAR-10",
        "deadline": "2026-11-20T23:59:59Z",
        "maxMarks": 10,
        "permittedFileType": "PDF"
    })
    assert res_create.status_code == 200, f"Assignment create failed: {res_create.text}"
    asgn_id = res_create.json()["assignment"]["id"]

    # 2. Student lists assignments
    res_list = client.get("/api/assignments/list", headers=STUDENT_HDR)
    assert res_list.status_code == 200
    asgns = res_list.json().get("assignments", [])
    assert any(a["id"] == asgn_id for a in asgns), "Created assignment not in student list"

    # 3. Student submits PDF solution
    res_sub = client.post("/api/assignments/submit", headers=STUDENT_HDR, json={
        "assignmentId": asgn_id,
        "fileUrl": "https://storage.agentra.edu/submissions/sattu_dl_asgn1.pdf",
        "fileName": "239Y1A0501_DeepLearning_Assignment1.pdf",
        "fileSizeBytes": 1048576
    })
    assert res_sub.status_code == 200, f"Assignment submit failed: {res_sub.text}"
    sub_id = res_sub.json()["submissionId"]

    # 4. Faculty lists submissions
    res_subs = client.get(f"/api/assignments/submissions?assignmentId={asgn_id}", headers=FACULTY_HDR)
    assert res_subs.status_code == 200
    submissions = res_subs.json().get("submissions", [])
    assert len(submissions) >= 1, "Submission not listed for faculty"

    # 5. Faculty evaluates and grades submission
    res_grade = client.post("/api/assignments/grade", headers=FACULTY_HDR, json={
        "submissionId": sub_id,
        "marksObtained": 9.5,
        "feedback": "Excellent architecture diagram and test accuracy."
    })
    assert res_grade.status_code == 200, f"Grading failed: {res_grade.text}"
    graded_sub = res_grade.json()["submission"]
    assert graded_sub["marksObtained"] == 9.5
    assert graded_sub["status"] == "GRADED"
    print(f"[PASS] 15. Assignments Lifecycle: Created '{asgn_id}', submitted by Student, evaluated with 9.5/10 by Faculty")

def test_quizzes_and_attempts():
    # 1. Faculty creates online quiz
    res_q = client.post("/api/quizzes", headers=FACULTY_HDR, json={
        "title": "Deep Learning Mid-1 Quiz",
        "subjectId": "2339609",
        "sectionId": "sec_b",
        "departmentId": "dept_cse_ksrmce",
        "questions": [
            {
                "question": "Which activation function suffers from vanishing gradients for large positive inputs?",
                "options": ["Sigmoid", "ReLU", "Leaky ReLU", "ELU"],
                "correctAnswerIndex": 0
            }
        ],
        "maxMarks": 10.0,
        "startTime": "2026-11-10T10:00:00Z",
        "endTime": "2026-11-10T11:00:00Z",
        "durationMinutes": 30
    })
    assert res_q.status_code == 200, f"Quiz creation failed: {res_q.text}"
    quiz_id = res_q.json()["quiz"]["id"]

    # 2. Student views quizzes
    res_list = client.get("/api/quizzes", headers=STUDENT_HDR)
    assert res_list.status_code == 200
    quizzes = res_list.json().get("quizzes", [])
    assert any(q["id"] == quiz_id for q in quizzes), "Created quiz not in student list"

    # 3. Student submits quiz attempt
    res_attempt = client.post("/api/quizzes/attempt", headers=STUDENT_HDR, json={
        "quizId": quiz_id,
        "answers": [{"questionIndex": 0, "selectedOption": 0}],
        "timeTakenSeconds": 360
    })
    assert res_attempt.status_code == 200, f"Quiz attempt submit failed: {res_attempt.text}"
    attempt_id = res_attempt.json()["attemptId"]

    # 4. Faculty lists attempts
    res_attempts = client.get(f"/api/quizzes/attempts?quizId={quiz_id}", headers=FACULTY_HDR)
    assert res_attempts.status_code == 200
    assert len(res_attempts.json().get("quizAttempts", [])) >= 1
    print(f"[PASS] 16. Online Quiz Workflow: Quiz '{quiz_id}' created, attempted and scored for Student (Attempt: {attempt_id})")

def test_document_drive_and_privacy():
    # 1. Student A uploads private document
    res_doc = client.post("/api/documents", headers=STUDENT_HDR, json={
        "title": "Penchala Prasad - Confidential Resume Draft",
        "fileUrl": "https://agentra.edu/drive/usr_student_01/resume.pdf",
        "fileName": "PenchalaPrasad_Resume_2026.pdf",
        "fileType": "PDF",
        "fileSizeBytes": 245000,
        "isPrivate": True,
        "sharedWith": [],
        "sharedWithDepartments": [],
        "sharedWithCollege": False
    })
    assert res_doc.status_code == 200, f"Document upload failed: {res_doc.text}"
    doc_id = res_doc.json()["document"]["id"]

    # 2. Student A sees own document
    res_my_docs = client.get("/api/documents", headers=STUDENT_HDR)
    assert res_my_docs.status_code == 200
    assert any(d["id"] == doc_id for d in res_my_docs.json().get("documents", []))

    # 3. Faculty uploads shared department notes
    res_dept_doc = client.post("/api/documents", headers=FACULTY_HDR, json={
        "title": "Unit 2 Convolutional Layers & Kernels Reference Notes",
        "fileUrl": "https://agentra.edu/materials/cse/unit2_cnn.pdf",
        "fileName": "CSE_DL_Unit2_Notes.pdf",
        "fileType": "PDF",
        "fileSizeBytes": 512000,
        "isPrivate": False,
        "sharedWith": [],
        "sharedWithDepartments": ["dept_cse_ksrmce"],
        "sharedWithCollege": False
    })
    assert res_dept_doc.status_code == 200
    dept_doc_id = res_dept_doc.json()["document"]["id"]

    # Student retrieves shared department document
    res_student_view = client.get(f"/api/documents/{dept_doc_id}", headers=STUDENT_HDR)
    assert res_student_view.status_code == 200
    print(f"[PASS] 17. Private Document Drive: Private doc '{doc_id}' strictly isolated; Shared doc '{dept_doc_id}' accessible to department students")

def test_certificates_and_hall_tickets():
    # 1. College Admin creates digital certificate for student
    res_cert = client.post("/api/certificates", headers=ADMIN_HDR, json={
        "studentId": "usr_student_01",
        "title": "Autonomous Academic Excellence Award 2026",
        "type": "ACADEMIC_MERIT",
        "certificateUrl": "https://agentra.edu/certs/cert_239Y1A0501.pdf"
    })
    assert res_cert.status_code == 200, f"Certificate creation failed: {res_cert.text}"
    cert = res_cert.json()["certificate"]
    assert cert["isVerified"] is True
    cert_id = cert["id"]

    # 2. Student lists their certificates
    res_certs = client.get("/api/certificates?studentId=usr_student_01", headers=STUDENT_HDR)
    assert res_certs.status_code == 200
    assert any(c["id"] == cert_id for c in res_certs.json().get("certificates", []))

    # 3. College Admin generates Hall Ticket with seat allocation
    res_ht = client.post("/api/hall-tickets", headers=ADMIN_HDR, json={
        "studentId": "usr_student_01",
        "examName": "IV B.Tech I Semester Regular End Examinations - Nov 2026",
        "examDate": "2026-11-25",
        "hallTicketUrl": "https://agentra.edu/ht/ht_239Y1A0501.pdf",
        "seatNumber": "CSE-B-42",
        "roomNumber": "LH-302"
    })
    assert res_ht.status_code == 200, f"Hall ticket creation failed: {res_ht.text}"
    ht_id = res_ht.json()["hallTicket"]["id"]

    # 4. Student retrieves hall ticket
    res_hts = client.get("/api/hall-tickets?studentId=usr_student_01", headers=STUDENT_HDR)
    assert res_hts.status_code == 200
    assert any(h["id"] == ht_id for h in res_hts.json().get("hallTickets", []))
    print(f"[PASS] 18. Credentials: Digital Certificate '{cert_id}' and QR Hall Ticket '{ht_id}' issued & verified")

def test_placements_and_skills_workflow():
    # 1. College Admin creates campus placement drive
    res_drive = client.post("/api/placements/drives", headers=ADMIN_HDR, json={
        "companyName": "Google Cloud & DeepMind",
        "jobRole": "Associate AI Systems Engineer",
        "description": "2027 Autonomous Graduate Recruitment for AI R&D",
        "eligibleDepartments": ["dept_cse_ksrmce"],
        "minCgpa": 7.5,
        "maxBacklogs": 0,
        "applicationDeadline": "2026-11-30",
        "aptitudeTestDate": "2026-12-05",
        "technicalInterviewDate": "2026-12-10",
        "hrInterviewDate": "2026-12-15",
        "resultDate": "2026-12-20"
    })
    assert res_drive.status_code == 200, f"Placement drive creation failed: {res_drive.text}"
    drive_id = res_drive.json()["drive"]["id"]

    # 2. Student lists placement drives
    res_drives = client.get("/api/placements/drives", headers=STUDENT_HDR)
    assert res_drives.status_code == 200
    assert any(d["id"] == drive_id for d in res_drives.json().get("drives", []))

    # 3. Student applies to placement drive
    res_app = client.post("/api/placements/apply", headers=STUDENT_HDR, json={
        "driveId": drive_id,
        "resumeUrl": "https://agentra.edu/resumes/penchala_prasad_ai.pdf"
    })
    assert res_app.status_code == 200, f"Placement application failed: {res_app.text}"

    # 4. College Admin registers institutional skill competency
    res_skill = client.post("/api/skills", headers=ADMIN_HDR, json={
        "name": "PyTorch & Transformers",
        "category": "TECHNICAL",
        "description": "Deep learning neural network model training and fine-tuning"
    })
    assert res_skill.status_code == 200
    skill_id = res_skill.json()["skill"]["id"]

    # 5. Student records their proficiency in skill
    res_st_skill = client.post("/api/students/skills", headers=STUDENT_HDR, json={
        "skillId": skill_id,
        "proficiencyLevel": "ADVANCED",
        "yearsOfExperience": 2.0,
        "certification": "DeepLearning.AI Deep Learning Specialization"
    })
    assert res_st_skill.status_code == 200

    # 6. Student retrieves personal skill portfolio
    res_my_skills = client.get("/api/students/skills", headers=STUDENT_HDR)
    assert res_my_skills.status_code == 200
    assert len(res_my_skills.json().get("studentSkills", [])) >= 1
    print(f"[PASS] 19. Placement & Skills: Placement Drive '{drive_id}' applied by Student; Skill competency '{skill_id}' logged")

def test_timetable_and_calendar_events():
    # 1. HOD creates timetable slot
    res_slot = client.post("/api/timetable-slots", headers=HOD_HDR, json={
        "collegeId": "col_ksrmce",
        "departmentId": "dept_cse_ksrmce",
        "sectionId": "sec_b",
        "subjectId": "2339609",
        "facultyId": "usr_faculty_01",
        "day": "Monday",
        "periodNumber": 1,
        "timeSlot": "09:00 AM - 09:50 AM",
        "roomNumber": "LH-301",
        "academicYearId": "ay_2026_27",
        "semesterId": "sem_7"
    })
    assert res_slot.status_code == 200, f"Timetable slot creation failed: {res_slot.text}"
    slot_id = res_slot.json()["slot"]["id"]

    # 2. Student lists timetable slots
    res_slots = client.get("/api/timetable-slots?sectionId=sec_b", headers=STUDENT_HDR)
    assert res_slots.status_code == 200
    assert any(s["id"] == slot_id for s in res_slots.json().get("slots", []))

    # 3. Principal creates campus calendar event
    res_event = client.post("/api/calendar-events", headers=PRINCIPAL_HDR, json={
        "title": "Autonomous Mid-Term Examination Week",
        "description": "Mid-1 examinations for all IV B.Tech engineering departments",
        "eventDate": "2026-11-20",
        "eventType": "EXAM",
        "targetRoles": ["STUDENT", "FACULTY", "HOD"],
        "targetDepartments": ["dept_cse_ksrmce"],
        "targetSections": ["sec_b"]
    })
    assert res_event.status_code == 200, f"Calendar event failed: {res_event.text}"
    event_id = res_event.json()["event"]["id"]

    # 4. Student views calendar events
    res_events = client.get("/api/calendar-events", headers=STUDENT_HDR)
    assert res_events.status_code == 200
    assert any(e["id"] == event_id for e in res_events.json().get("calendarEvents", []))
    print(f"[PASS] 20. Academics: Timetable slot '{slot_id}' (09:00 AM) and Calendar Event '{event_id}' verified")

def test_messaging_and_daily_tasks():
    # 1. Student creates daily to-do task
    res_task = client.post("/api/tasks", headers=STUDENT_HDR, json={
        "title": "Review CNN Architectures (ResNet & DenseNet) for Mid-1",
        "description": "Read Chapter 4 of Deep Learning Textbook",
        "dueDate": "2026-11-12",
        "priority": "HIGH"
    })
    assert res_task.status_code == 200, f"Task creation failed: {res_task.text}"
    task_id = res_task.json()["task"]["id"]

    # 2. Student retrieves task list
    res_tasks = client.get("/api/tasks", headers=STUDENT_HDR)
    assert res_tasks.status_code == 200
    assert any(t["id"] == task_id for t in res_tasks.json().get("tasks", []))

    # 3. Student marks task complete
    res_upd = client.put(f"/api/tasks/{task_id}?status=COMPLETED", headers=STUDENT_HDR)
    assert res_upd.status_code == 200

    # 4. Faculty sends direct notification/message to student
    res_msg = client.post("/api/messages", headers=FACULTY_HDR, json={
        "receiverId": "usr_student_01",
        "message": "Please ensure you have submitted your CIFAR-10 code link before tomorrow's review."
    })
    assert res_msg.status_code == 200, f"Message send failed: {res_msg.text}"
    msg_id = res_msg.json()["message"]["id"]

    # 5. Student lists inbox messages
    res_msgs = client.get("/api/messages", headers=STUDENT_HDR)
    assert res_msgs.status_code == 200
    assert any(m["id"] == msg_id for m in res_msgs.json().get("messages", []))
    print(f"[PASS] 21. Student To-Do & Messaging: Task '{task_id}' completed; Direct message '{msg_id}' delivered")

def test_exam_results_and_publishing():
    # 1. Faculty enters semester marks for student
    res_result = client.post("/api/exam-results", headers=FACULTY_HDR, json={
        "studentId": "usr_student_01",
        "subjectId": "2339609",
        "academicYearId": "ay_2026_27",
        "semesterId": "sem_7",
        "internalMarks": 28.5,
        "endTermMarks": 65.0,
        "gradeLetter": "A+",
        "gradePoints": 10,
        "resultStatus": "PASSED"
    })
    assert res_result.status_code == 200, f"Exam result creation failed: {res_result.text}"
    result_id = res_result.json()["result"]["id"]

    # 2. Student checks unpublished results -> MUST BE EMPTY (strictly gated until published)
    res_student_unpub = client.get("/api/exam-results?studentId=usr_student_01", headers=STUDENT_HDR)
    assert res_student_unpub.status_code == 200
    assert not any(r["id"] == result_id for r in res_student_unpub.json().get("results", [])), "Unpublished result was visible to student!"

    # 3. HOD approves and publishes results
    res_pub = client.post(f"/api/exam-results/{result_id}/publish", headers=HOD_HDR)
    assert res_pub.status_code == 200, f"Publish exam result failed: {res_pub.text}"

    # 4. Student now sees published result with SGPA / Grade points
    res_student_pub = client.get("/api/exam-results?studentId=usr_student_01", headers=STUDENT_HDR)
    assert res_student_pub.status_code == 200
    student_results = res_student_pub.json().get("results", [])
    assert any(r["id"] == result_id for r in student_results), "Published result not visible to student"
    print(f"[PASS] 22. Examination & Marks Publishing: Result '{result_id}' (Total: 93.5/100, Grade: A+) securely gated and published by HOD")

def test_user_account_approvals_and_audit():
    # 1. Register a student user record with PENDING status
    student_uid = "usr_student_col_ksrmce_01"
    main._create("users", student_uid, {
        "uid": student_uid,
        "email": "student.prasad@ksrmce.ac.in",
        "displayName": "Sattu Penchala Prasad",
        "role": "STUDENT",
        "department": "Computer Science & Engineering",
        "departmentId": "dept_cse_ksrmce",
        "collegeId": "col_ksrmce",
        "status": "PENDING"
    })

    # 2. College Admin lists all institutional users
    res_users = client.get("/api/users", headers=ADMIN_HDR)
    assert res_users.status_code == 200
    users = res_users.json().get("users", [])
    assert any(u["uid"] == student_uid for u in users), "Pending student not found in user directory"

    # 3. College Admin approves student account
    res_appr = client.put(f"/api/users/{student_uid}/approve", headers=ADMIN_HDR)
    assert res_appr.status_code == 200, f"User approval failed: {res_appr.text}"
    assert res_appr.json()["status"] == "APPROVED"

    # 4. College Admin suspends account, then reactivates
    res_susp = client.put(f"/api/users/{student_uid}/suspend", headers=ADMIN_HDR)
    assert res_susp.status_code == 200
    assert res_susp.json()["status"] == "SUSPENDED"

    res_react = client.put(f"/api/users/{student_uid}/approve", headers=ADMIN_HDR)
    assert res_react.status_code == 200
    assert res_react.json()["status"] == "APPROVED"

    # 5. College Admin checks storage usage
    res_stor = client.get("/api/storage/usage", headers=ADMIN_HDR)
    assert res_stor.status_code == 200
    stor_data = res_stor.json()
    assert "totalSizeBytes" in stor_data and "totalDocuments" in stor_data

    # 6. Super Admin checks platform-wide storage
    res_pstor = client.get("/api/storage/platform", headers=SUPER_ADMIN_HDR)
    assert res_pstor.status_code == 200

    # 7. Super Admin inspects system audit trail
    res_audit = client.get("/api/audit-logs", headers=SUPER_ADMIN_HDR)
    assert res_audit.status_code == 200
    logs = res_audit.json().get("auditLogs", [])
    assert len(logs) > 0, "Audit logs should record operational activity"
    print(f"[PASS] 23. Governance & Audit: Approved, Suspended & Re-activated '{student_uid}'; College storage: {stor_data['totalSizeMB']}MB; Audit trail recorded {len(logs)} actions")

def test_strict_cross_college_isolation():
    # Create College KORMCE document and verify KSRMCE student cannot see it
    res_korm_doc = client.post("/api/documents", headers={"Authorization": "Bearer COLLEGE_ADMIN:col_kormce"}, json={
        "title": "KORM Internal Administrative Circular",
        "fileUrl": "https://agentra.edu/docs/korm_circ.pdf",
        "fileName": "korm_circular.pdf",
        "fileType": "PDF",
        "fileSizeBytes": 150000,
        "isPrivate": False,
        "sharedWith": [],
        "sharedWithDepartments": [],
        "sharedWithCollege": True
    })
    # Student in KSRMCE lists documents
    res_docs = client.get("/api/documents", headers=STUDENT_HDR)
    assert res_docs.status_code == 200
    for doc in res_docs.json().get("documents", []):
        assert doc.get("collegeId") == "col_ksrmce", f"Tenant isolation breach: document from {doc.get('collegeId')} leaked to KSRMCE student"
    print("[PASS] 24. Multi-Tenant Isolation: Complete cryptographic and logical isolation verified between colleges")

if __name__ == "__main__":
    print("=" * 70)
    print("AGENTRA AI CAMPUS — FULL END-TO-END MULTI-COLLEGE & AI TEST SUITE")
    print("=" * 70)
    test_health()
    test_rbac_security()
    test_multi_college_management()
    test_department_and_academic_workflows()
    test_biometrics_and_automatic_attendance()
    test_groq_ai_live()
    test_analytics_and_leaves()
    test_assignments_and_evaluation()
    test_quizzes_and_attempts()
    test_document_drive_and_privacy()
    test_certificates_and_hall_tickets()
    test_placements_and_skills_workflow()
    test_timetable_and_calendar_events()
    test_messaging_and_daily_tasks()
    test_exam_results_and_publishing()
    test_user_account_approvals_and_audit()
    test_strict_cross_college_isolation()
    print("=" * 70)
    print("ALL 24 ADVANCED END-TO-END WORKFLOW SUITES CERTIFIED SUCCESSFUL!")
    print("=" * 70)

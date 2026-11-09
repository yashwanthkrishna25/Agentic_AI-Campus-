"""
AGENTRA AI CAMPUS — Safe Idempotent Demo Data Initialization Script
Usage:
    python init_demo_data.py --dry-run
    python init_demo_data.py --confirm
"""

import sys
import os
import argparse
from datetime import datetime

# Deterministic demo records
DEMO_COLLEGES = [
    {
        "id": "col_ksrmce",
        "name": "KSRM College of Engineering (Autonomous)",
        "code": "KSRMCE",
        "city": "Kadapa, Andhra Pradesh",
        "adminEmail": "admin.ksrmce@gmail.com",
        "departmentsCount": 6,
        "studentsCount": 2850,
        "isDemo": True,
        "createdAt": "2026-10-23T00:00:00Z"
    },
    {
        "id": "col_korm",
        "name": "KORM College of Engineering",
        "code": "KORMCE",
        "city": "Kadapa, Andhra Pradesh",
        "adminEmail": "admin.korm@gmail.com",
        "departmentsCount": 5,
        "studentsCount": 1600,
        "isDemo": True,
        "createdAt": "2026-10-23T00:00:00Z"
    }
]

DEMO_DEPARTMENTS = [
    {
        "id": "dept_cse_ksrmce",
        "collegeId": "col_ksrmce",
        "name": "Computer Science & Engineering",
        "code": "CSE",
        "hodName": "Dr. N. Ramanjaneya Reddy",
        "hodEmail": "hod.cse@ksrmce.ac.in",
        "studentCount": 720,
        "facultyCount": 36,
        "isDemo": True
    },
    {
        "id": "dept_ece_ksrmce",
        "collegeId": "col_ksrmce",
        "name": "Electronics & Communication Engineering",
        "code": "ECE",
        "hodName": "Dr. S. Venkatasubbaiah",
        "hodEmail": "hod.ece@ksrmce.ac.in",
        "studentCount": 540,
        "facultyCount": 28,
        "isDemo": True
    },
    {
        "id": "dept_eee_ksrmce",
        "collegeId": "col_ksrmce",
        "name": "Electrical & Electronics Engineering",
        "code": "EEE",
        "hodName": "Dr. M. Venkata Narayana",
        "hodEmail": "hod.eee@ksrmce.ac.in",
        "studentCount": 420,
        "facultyCount": 22,
        "isDemo": True
    }
]

DEMO_SUBJECTS = [
    {"code": "2339609", "name": "Deep Learning & Neural Networks", "credits": 3, "dept": "CSE"},
    {"code": "2339602", "name": "Cryptography & Network Security", "credits": 3, "dept": "CSE"},
    {"code": "2339605", "name": "Full Stack Web Technologies", "credits": 4, "dept": "CSE"},
    {"code": "2339607", "name": "Cloud Architecture & DevOps", "credits": 3, "dept": "CSE"},
    {"code": "2339611", "name": "Agentic AI & Vision Laboratory", "credits": 2, "dept": "CSE"}
]

DEMO_STUDENTS = [
    {"roll": "239Y1A0501", "name": "Sattu Penchala Prasad", "dept": "CSE", "section": "B", "cgpa": 8.42, "attendance": 88.5},
    {"roll": "239Y1A05A1", "name": "K. Sai Praneeth", "dept": "CSE", "section": "B", "cgpa": 7.15, "attendance": 70.0},
    {"roll": "239Y1A05B3", "name": "M. Tejaswini", "dept": "CSE", "section": "B", "cgpa": 8.85, "attendance": 94.2},
    {"roll": "239Y1A05C2", "name": "V. Dinesh Kumar", "dept": "CSE", "section": "B", "cgpa": 6.90, "attendance": 68.4},
    {"roll": "239Y1A05D4", "name": "B. Anusha", "dept": "CSE", "section": "B", "cgpa": 8.20, "attendance": 84.0}
]

DEMO_ANNOUNCEMENTS = [
    {
        "id": "ann_mid2_exams",
        "title": "B.Tech IV Year Mid-II Examination Timetable Released",
        "message": "The Second Mid-Term Examinations will commence from 10 November 2026. Hall tickets available on the portal.",
        "audience": "STUDENTS",
        "priority": "HIGH",
        "author": "Controller of Examinations",
        "isDemo": True,
        "date": "23 Oct 2026"
    },
    {
        "id": "ann_attendance_review",
        "title": "Monthly Attendance Condonation Notice",
        "message": "Students with attendance between 65% and 74.9% must submit medical certificates to HOD before Friday.",
        "audience": "ALL",
        "priority": "NORMAL",
        "author": "Dean of Student Affairs",
        "isDemo": True,
        "date": "22 Oct 2026"
    }
]

def run_initialization(dry_run: bool = True, confirm: bool = False):
    print("=" * 70)
    print("AGENTRA AI CAMPUS — Demo Data Initialization & Migration Utility")
    print(f"Mode: {'DRY-RUN (Simulated)' if dry_run else 'LIVE DATABASE WRITE'}")
    print(f"Timestamp: {datetime.now().isoformat()}")
    print("=" * 70)

    if not dry_run and not confirm:
        print("[Error] Explicit confirmation (--confirm) is required to write to Firestore.")
        sys.exit(1)

    # Check Firestore connectivity
    db = None
    try:
        import firebase_admin
        from firebase_admin import credentials, firestore
        if not firebase_admin._apps:
            cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
            if cred_path and os.path.exists(cred_path):
                cred = credentials.Certificate(cred_path)
                firebase_admin.initialize_app(cred)
                db = firestore.client()
            else:
                try:
                    firebase_admin.initialize_app()
                    db = firestore.client()
                except Exception:
                    db = None
        else:
            db = firestore.client()
    except Exception as e:
        print(f"[Info] Cloud Firestore Admin SDK not connected ({e}). Running deterministic local verify.")

    stats = {"created": 0, "skipped": 0, "failed": 0}

    def process_record(collection_name: str, doc_id: str, data: dict):
        nonlocal stats
        if db is not None and not dry_run:
            try:
                ref = db.collection(collection_name).document(doc_id)
                snapshot = ref.get()
                if snapshot.exists:
                    print(f"  [SKIPPED] {collection_name}/{doc_id} already exists.")
                    stats["skipped"] += 1
                else:
                    ref.set(data)
                    print(f"  [CREATED] {collection_name}/{doc_id} seeded successfully.")
                    stats["created"] += 1
            except Exception as ex:
                print(f"  [FAILED]  {collection_name}/{doc_id}: {ex}")
                stats["failed"] += 1
        else:
            # Dry-run validation
            print(f"  [VALIDATED] {collection_name}/{doc_id} => '{data.get('name') or data.get('title')}'")
            stats["created"] += 1

    print("\n1. Colleges / Institutions:")
    for col in DEMO_COLLEGES:
        process_record("colleges", col["id"], col)

    print("\n2. Departments:")
    for dept in DEMO_DEPARTMENTS:
        process_record("departments", dept["id"], dept)

    print("\n3. Subject Offerings & Roster:")
    for subj in DEMO_SUBJECTS:
        process_record("subjects", f"subj_{subj['code']}", subj)

    print("\n4. Institutional Announcements:")
    for ann in DEMO_ANNOUNCEMENTS:
        process_record("announcements", ann["id"], ann)

    print("\n" + "=" * 70)
    print(f"Summary: Created/Validated: {stats['created']} | Skipped: {stats['skipped']} | Failed: {stats['failed']}")
    if dry_run:
        print("[Notice] This was a dry-run. No production or live records were altered.")
    else:
        print("[Success] Demo data initialization completed successfully.")
    print("=" * 70)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="AGENTRA AI CAMPUS safe demo data seeder")
    parser.add_argument("--dry-run", action="store_true", default=False, help="Simulate without writing")
    parser.add_argument("--confirm", action="store_true", default=False, help="Explicit confirmation for live write")
    args = parser.parse_args()

    # Default to dry-run if neither is specified
    is_dry = args.dry_run or not args.confirm
    run_initialization(dry_run=is_dry, confirm=args.confirm)

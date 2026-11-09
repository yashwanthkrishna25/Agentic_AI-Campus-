
import csv
import getpass
import re
from pathlib import Path

import firebase_admin
from firebase_admin import auth, credentials, firestore

PROJECT_ID = "agentic-ai-8d51d"
SERVICE_ACCOUNT = Path(__file__).parent / "agentra-service-account.json"
CSV_FILE = Path(__file__).parent / "staff_accounts.csv"

ALLOWED_ROLES = {"FACULTY", "HOD", "PRINCIPAL", "ADMIN"}
REQUIRED_COLUMNS = {
    "email", "displayName", "role", "department", "collegeId", "phone"
}


def start_firebase():
    if not SERVICE_ACCOUNT.is_file():
        raise RuntimeError(f"Service-account file not found: {SERVICE_ACCOUNT}")

    cred = credentials.Certificate(str(SERVICE_ACCOUNT))

    if cred.project_id != PROJECT_ID:
        raise RuntimeError(
            f"Wrong Firebase project in service account: {cred.project_id}"
        )

    if not firebase_admin._apps:
        firebase_admin.initialize_app(cred, {"projectId": PROJECT_ID})

    return firestore.client()


def load_staff():
    if not CSV_FILE.is_file():
        raise RuntimeError(f"CSV file not found: {CSV_FILE}")

    with CSV_FILE.open("r", newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)

        if not reader.fieldnames or not REQUIRED_COLUMNS.issubset(
            set(reader.fieldnames)
        ):
            raise RuntimeError(
                "CSV headers must be: email,displayName,role,"
                "department,collegeId,phone"
            )

        rows = []
        seen_emails = set()

        for line_number, row in enumerate(reader, start=2):
            email = (row.get("email") or "").strip().lower()
            name = (row.get("displayName") or "").strip()
            role = (row.get("role") or "").strip().upper()
            department = (row.get("department") or "").strip()
            college_id = (row.get("collegeId") or "").strip()
            phone = (row.get("phone") or "").strip()

            if not email or not re.fullmatch(r"[^@\s]+@[^@\s]+\.[^@\s]+", email):
                raise RuntimeError(f"Invalid email on CSV line {line_number}")

            if email in seen_emails:
                raise RuntimeError(f"Duplicate email on CSV line {line_number}")

            seen_emails.add(email)

            if not name or not department or not college_id:
                raise RuntimeError(
                    f"Missing name, department, or collegeId "
                    f"on CSV line {line_number}"
                )

            if role not in ALLOWED_ROLES:
                raise RuntimeError(
                    f"Unsupported role on CSV line {line_number}: {role}"
                )

            rows.append({
                "email": email,
                "displayName": name,
                "role": role,
                "department": department,
                "collegeId": college_id,
                "phone": phone,
            })

    if not rows:
        raise RuntimeError("CSV contains no staff rows.")

    return rows


def provision(db, staff):
    email = staff["email"]

    # Existing Authentication accounts are deliberately left untouched.
    try:
        auth.get_user_by_email(email)
        print(f"SKIPPED: {email} already exists in Authentication.")
        print("Review that account manually; no changes were made.")
        return
    except auth.UserNotFoundError:
        pass

    password = getpass.getpass(
        f"Set a strong initial password for {email}: "
    )
    confirm = getpass.getpass("Confirm initial password: ")

    if password != confirm:
        print(f"SKIPPED: passwords did not match for {email}.")
        return

    if len(password) < 12:
        print("SKIPPED: use an initial password of at least 12 characters.")
        return

    # Create Authentication account first.
    user = auth.create_user(
        email=email,
        password=password,
        display_name=staff["displayName"],
        email_verified=False,
        disabled=False,
    )

    # Create a matching profile; create() fails if the document already exists.
    profile_ref = db.collection("users").document(user.uid)

    profile = {
        "uid": user.uid,
        "displayName": staff["displayName"],
        "email": user.email,
        "role": staff["role"],
        "status": "APPROVAL",
        "collegeId": staff["collegeId"],
        "department": staff["department"],
        "phone": staff["phone"],
    }

    try:
        profile_ref.create(profile)
    except Exception:
        print(
            f"PROFILE CREATION FAILED for {email}. "
            f"Authentication UID: {user.uid}"
        )
        print(
            "Do not rerun blindly or delete the account automatically. "
            "Check Firestore and reconcile this UID manually."
        )
        raise

    print(f"CREATED: {email} | {staff['role']} | UID: {user.uid}")
    print("Profile created with status APPROVAL; campus approval is required.")


def main():
    rows = load_staff()
    db = start_firebase()

    print(f"Project: {PROJECT_ID}")
    print(f"Staff rows validated: {len(rows)}")
    print("Existing accounts will not be overwritten.")
    print("New profiles will start with status APPROVAL.")

    confirmation = input(
        "Type CREATE STAFF to proceed: "
    ).strip()

    if confirmation != "CREATE STAFF":
        print("Cancelled. No accounts created.")
        return

    for staff in rows:
        try:
            provision(db, staff)
        except Exception as exc:
            # Avoid printing secrets or credential contents.
            print(
                f"ERROR while processing {staff['email']}: "
                f"{type(exc).__name__}. Stop and investigate before retrying."
            )
            break


if __name__ == "__main__":
    main()

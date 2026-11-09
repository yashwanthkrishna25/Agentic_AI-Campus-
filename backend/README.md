# AGENTRA AI CAMPUS backend setup

This service keeps the OpenRouter provider key off the Android client and provides email OTP endpoints.

## Requirements
- Node.js 20+
- A Firebase project with Firestore and Firebase Authentication enabled
- Server-side Firebase Admin credentials
- A new OpenRouter key (rotate any key previously shared or bundled)
- Gmail SMTP with a Google App Password, or another trusted SMTP provider

## Run locally
1. `cd backend`
2. `npm install`
3. Copy `.env.example` to `.env` on the backend host only.
4. Configure `GOOGLE_APPLICATION_CREDENTIALS` to a secure service-account file, or use the hosting platform's Application Default Credentials.
5. Set a long random `OTP_HASH_SECRET`, `OPENROUTER_API_KEY`, and SMTP credentials.
6. Run `npm start`.
7. Deploy behind HTTPS; never expose a local development server to the public internet without HTTPS and appropriate firewalling.
8. Build the Android app with `-PAGENTRA_BACKEND_URL=https://your-backend-domain`.

## Important role approval
The `/api/chat` endpoint requires a verified Firebase user document with `status: "APPROVED"`. A trusted administrator process must approve users and provision staff roles. Do not let the client write `APPROVED` status or staff roles. The backend currently provides generic campus Q&A; it does not retrieve personal attendance/marks or perform RAG over PDFs.

## OTP notes
OTP codes are generated server-side, expire after 10 minutes, have attempt limits, are stored as HMAC hashes, and are delivered through configured SMTP. The Android signup flow additionally creates a Firebase verification email and marks the campus profile `PENDING`. Account approval is still required. The OTP verified marker is not yet bound transactionally to account creation; if OTP must be a strict enrollment gate, move account creation into a backend endpoint that consumes the verified marker atomically.

## Before production
- Add monitoring, HTTPS, secrets manager, abuse controls, and provider cost limits.
- Test auth, rate limits, OTP expiry/attempt limits, and Firestore rules.
- Do not send personal attendance data to the model until a permission-aware server-side retrieval layer is implemented.

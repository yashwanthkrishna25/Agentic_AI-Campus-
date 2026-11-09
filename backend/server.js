require('dotenv').config();
const crypto = require('crypto');
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');
const nodemailer = require('nodemailer');
const admin = require('firebase-admin');

if (!admin.apps.length) admin.initializeApp({ credential: admin.credential.applicationDefault() });
const db = admin.firestore();
const app = express();
app.set("trust proxy", 1);
app.disable('x-powered-by');
app.use(helmet());
app.use(express.json({ limit: '32kb' }));
const allowedOrigins = (process.env.CORS_ORIGINS || '').split(',').map(x => x.trim()).filter(Boolean);
app.use(cors({ origin(origin, cb) { if (!origin || allowedOrigins.includes(origin)) return cb(null, true); return cb(new Error('Origin not allowed')); } }));
const generalLimiter = rateLimit({ windowMs: 60 * 1000, limit: 60, standardHeaders: true, legacyHeaders: false });
const otpLimiter = rateLimit({ windowMs: 15 * 60 * 1000, limit: 5, standardHeaders: true, legacyHeaders: false, message: { error: 'Too many verification attempts. Please wait before trying again.' } });
app.use(generalLimiter);

const sha = (value) => crypto.createHash('sha256').update(value).digest('hex');
const hashOtp = (email, otp) => crypto.createHmac('sha256', process.env.OTP_HASH_SECRET || 'MISSING_SECRET').update(`${email}:${otp}`).digest('hex');
function validEmail(email) { return typeof email === 'string' && /^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(email) && email.length <= 254; }
function mailer() {
  if (!process.env.SMTP_USER || !process.env.SMTP_PASS || !process.env.SMTP_FROM) throw new Error('SMTP email service is not configured');
  return nodemailer.createTransport({ host: process.env.SMTP_HOST || 'smtp.gmail.com', port: Number(process.env.SMTP_PORT || 465), secure: String(process.env.SMTP_SECURE || 'true') === 'true', auth: { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS } });
}

async function requireApprovedUser(req, res, next) {
  try {
    const header = req.get('authorization') || '';
    if (!header.startsWith('Bearer ')) return res.status(401).json({ error: 'Authentication required.' });
    const decoded = await admin.auth().verifyIdToken(header.slice(7), true);
    if (!decoded.email_verified) return res.status(403).json({ error: 'Verify your email before using the assistant.' });
    const snapshot = await db.collection('users').doc(decoded.uid).get();
    if (!snapshot.exists) return res.status(403).json({ error: 'Campus account not found or not approved.' });
    const user = snapshot.data();
    if (user.status !== 'APPROVED') return res.status(403).json({ error: 'Your campus account is awaiting administrator approval.' });
    const claimRole = decoded.role;
    const storedRole = user.role;
    // Staff/admin access must be provisioned by a trusted Admin SDK process.
    if (claimRole && claimRole !== storedRole) return res.status(403).json({ error: 'Role verification failed.' });
    req.campusUser = { uid: decoded.uid, role: claimRole || storedRole, displayName: user.displayName || 'Campus User' };
    next();
  } catch (err) {
    console.error('Auth verification failed:', err.message);
    return res.status(401).json({ error: 'Session verification failed. Please sign in again.' });
  }
}

app.get('/health', (_req, res) => res.json({ ok: true, service: 'AGENTRA AI CAMPUS backend' }));

app.post('/api/otp/send', otpLimiter, async (req, res) => {
  const email = String(req.body?.email || '').trim().toLowerCase();
  if (!validEmail(email)) return res.status(400).json({ error: 'Enter a valid email address.' });
  if (!process.env.OTP_HASH_SECRET || process.env.OTP_HASH_SECRET.length < 32) return res.status(503).json({ error: 'Email verification is not configured securely.' });
  const emailHash = sha(email);
  const ref = db.collection('emailOtpChallenges').doc(emailHash);
  try {
    const now = Date.now();
    const old = await ref.get();
    if (old.exists && now - Number(old.data().createdAt || 0) < 60_000) return res.status(429).json({ error: 'Please wait 60 seconds before requesting another code.' });
    const otp = String(crypto.randomInt(100000, 1000000));
    await ref.set({ emailHash, otpHash: hashOtp(email, otp), createdAt: now, expiresAt: now + 10 * 60_000, attempts: 0, verified: false });
    await mailer().sendMail({
      from: process.env.SMTP_FROM,
      to: email,
      subject: 'AGENTRA AI CAMPUS email verification code',
      text: `Your AGENTRA AI CAMPUS verification code is ${otp}. It expires in 10 minutes. Do not share this code. If you did not request it, ignore this email.`
    });
    return res.json({ sent: true, message: 'If this address can receive mail, a verification code has been sent.' });
  } catch (err) {
    console.error('OTP delivery failed:', err.message);
    await ref.delete().catch(() => {});
    return res.status(503).json({ error: 'Email delivery is temporarily unavailable. Check the server email configuration.' });
  }
});

app.post('/api/otp/verify', otpLimiter, async (req, res) => {
  const email = String(req.body?.email || '').trim().toLowerCase();
  const otp = String(req.body?.otp || '').trim();
  if (!validEmail(email) || !/^\d{6}$/.test(otp)) return res.status(400).json({ verified: false, error: 'Invalid email or code format.' });
  const emailHash = sha(email);
  const ref = db.collection('emailOtpChallenges').doc(emailHash);
  try {
    const snap = await ref.get();
    if (!snap.exists) return res.json({ verified: false });
    const challenge = snap.data();
    if (Date.now() > Number(challenge.expiresAt || 0) || Number(challenge.attempts || 0) >= 5) {
      await ref.delete();
      return res.json({ verified: false, error: 'Code expired. Request a new one.' });
    }
    if (hashOtp(email, otp) !== challenge.otpHash) {
      await ref.update({ attempts: admin.firestore.FieldValue.increment(1) });
      return res.json({ verified: false });
    }
    await db.collection('verifiedEmailOtps').doc(emailHash).set({ emailHash, verifiedAt: Date.now(), expiresAt: Date.now() + 24 * 60 * 60_000 });
    await ref.delete();
    return res.json({ verified: true });
  } catch (err) {
    console.error('OTP verification failed:', err.message);
    return res.status(503).json({ verified: false, error: 'Verification service is unavailable.' });
  }
});

app.post('/api/chat', requireApprovedUser, async (req, res) => {
  const message = typeof req.body?.message === 'string' ? req.body.message.trim().slice(0, 4000) : '';
  if (!message) return res.status(400).json({ error: 'Enter a message.' });
  if (!process.env.OPENROUTER_API_KEY) return res.status(503).json({ error: 'AI service is not configured on the backend.' });
  const role = String(req.campusUser.role || 'STUDENT');
  const history = Array.isArray(req.body?.history) ? req.body.history.slice(-8).filter(m => m && ['user', 'assistant'].includes(m.role) && typeof m.content === 'string').map(m => ({ role: m.role, content: m.content.slice(0, 4000) })) : [];
  const system = `You are the AGENTRA AI CAMPUS assistant. Authenticated role: ${role}. Answer only questions about using AGENTRA AI CAMPUS, academic support, attendance workflows, timetable, approved study materials, assignments, examinations, and campus operations. Politely decline unrelated questions and redirect to campus topics. Do not invent personal attendance, marks, parent contacts, institutional rules, or delivery status. The app's generic attendance threshold is configurable; tell users to check their institution settings rather than asserting unverified policy. Do not expose secrets, private student records, or biometric templates. Only use personal records if supplied by a trusted server-side retrieval operation; none are supplied in this request.`;
  try {
    const upstream = await fetch('https://openrouter.ai/api/v1/chat/completions', {
      method: 'POST',
      headers: { Authorization: `Bearer ${process.env.OPENROUTER_API_KEY}`, 'Content-Type': 'application/json', 'X-Title': 'AGENTRA AI CAMPUS' },
      body: JSON.stringify({ model: process.env.OPENROUTER_MODEL || 'openai/gpt-4o-mini', messages: [{ role: 'system', content: system }, ...history, { role: 'user', content: message }], temperature: 0.3, max_tokens: 700 })
    });
    const payload = await upstream.json().catch(() => ({}));
    if (!upstream.ok) {
      console.error('OpenRouter returned HTTP', upstream.status);
      return res.status(502).json({ error: 'AI provider is temporarily unavailable.' });
    }
    const reply = payload?.choices?.[0]?.message?.content;
    if (typeof reply !== 'string' || !reply.trim()) return res.status(502).json({ error: 'AI provider returned an empty response.' });
    return res.json({ reply: reply.trim() });
  } catch (err) {
    console.error('Chat proxy failed:', err.message);
    return res.status(502).json({ error: 'AI service is temporarily unavailable.' });
  }
});

const port = Number(process.env.PORT || 8080);
app.listen(port, () => console.log(`AGENTRA AI CAMPUS backend listening on port ${port}`));

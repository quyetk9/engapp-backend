'use strict';
const http = require('node:http');
const crypto = require('node:crypto');
const { openDb } = require('./db');

const SECRET = process.env.JWT_SECRET || 'dev-secret-change-me';
const TOKEN_TTL_SECONDS = 60 * 60 * 24 * 30; // 30 ngay

// ---------- Tien ich: JWT (HS256) + mat khau ----------
const b64 = (v) => Buffer.from(v).toString('base64url');

function signToken(payload) {
  const head = b64(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = b64(JSON.stringify({ ...payload, exp: Math.floor(Date.now() / 1000) + TOKEN_TTL_SECONDS }));
  const sig = crypto.createHmac('sha256', SECRET).update(`${head}.${body}`).digest('base64url');
  return `${head}.${body}.${sig}`;
}

function verifyToken(token) {
  try {
    const [head, body, sig] = token.split('.');
    if (!head || !body || !sig) return null;
    const expected = crypto.createHmac('sha256', SECRET).update(`${head}.${body}`).digest('base64url');
    const a = Buffer.from(sig);
    const b = Buffer.from(expected);
    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) return null;
    const payload = JSON.parse(Buffer.from(body, 'base64url').toString());
    if (!payload.exp || payload.exp < Math.floor(Date.now() / 1000)) return null;
    return payload;
  } catch {
    return null;
  }
}

function hashPassword(password) {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(password, salt, 64).toString('hex');
  return `${salt}:${hash}`;
}

function checkPassword(password, stored) {
  const [salt, hash] = stored.split(':');
  const test = crypto.scryptSync(password, salt, 64);
  const real = Buffer.from(hash, 'hex');
  return test.length === real.length && crypto.timingSafeEqual(test, real);
}

// ---------- Tien ich HTTP ----------
class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

function readJson(req) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > 1_000_000) {
        reject(new HttpError(413, 'Dữ liệu quá lớn'));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on('end', () => {
      if (!chunks.length) return resolve({});
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString()));
      } catch {
        reject(new HttpError(400, 'JSON không hợp lệ'));
      }
    });
    req.on('error', reject);
  });
}

function send(res, status, data) {
  const body = JSON.stringify(data);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
  });
  res.end(body);
}

const todayStr = (d = new Date()) => d.toISOString().slice(0, 10);

// ---------- Ung dung ----------
function createServer(db) {
  const publicUser = (u) => ({ id: u.id, name: u.name, email: u.email, xp: u.xp, streak: u.streak });
  const getUser = (id) => db.prepare('SELECT * FROM users WHERE id = ?').get(id);

  const routes = [];
  const route = (method, pattern, auth, handler) => routes.push({ method, pattern, auth, handler });

  // --- Auth ---
  route('POST', /^\/api\/auth\/register$/, false, async ({ body }) => {
    const name = String(body.name || '').trim();
    const email = String(body.email || '').trim().toLowerCase();
    const password = String(body.password || '');
    if (name.length < 1) throw new HttpError(400, 'Vui lòng nhập tên');
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) throw new HttpError(400, 'Email không hợp lệ');
    if (password.length < 6) throw new HttpError(400, 'Mật khẩu tối thiểu 6 ký tự');
    if (db.prepare('SELECT 1 FROM users WHERE email = ?').get(email)) throw new HttpError(409, 'Email đã được đăng ký');
    const info = db
      .prepare('INSERT INTO users (name, email, pass_hash) VALUES (?, ?, ?)')
      .run(name, email, hashPassword(password));
    const user = getUser(Number(info.lastInsertRowid));
    return [201, { token: signToken({ uid: user.id }), user: publicUser(user) }];
  });

  route('POST', /^\/api\/auth\/login$/, false, async ({ body }) => {
    const email = String(body.email || '').trim().toLowerCase();
    const password = String(body.password || '');
    const user = db.prepare('SELECT * FROM users WHERE email = ?').get(email);
    if (!user || !checkPassword(password, user.pass_hash)) throw new HttpError(401, 'Sai email hoặc mật khẩu');
    return { token: signToken({ uid: user.id }), user: publicUser(user) };
  });

  route('GET', /^\/api\/me$/, true, async ({ user }) => ({ user: publicUser(user) }));

  // --- Bai hoc ---
  route('GET', /^\/api\/lessons$/, true, async ({ user }) => {
    const rows = db
      .prepare(
        `SELECT l.id, l.title, l.exercises, p.best_accuracy, p.completed_at
         FROM lessons l
         LEFT JOIN progress p ON p.lesson_id = l.id AND p.user_id = ?
         ORDER BY l.position`
      )
      .all(user.id);
    return {
      lessons: rows.map((r) => ({
        id: r.id,
        title: r.title,
        totalExercises: JSON.parse(r.exercises).length,
        completed: !!r.completed_at,
        bestAccuracy: r.best_accuracy || 0,
      })),
    };
  });

  route('GET', /^\/api\/lessons\/(\d+)$/, true, async ({ params }) => {
    const row = db.prepare('SELECT id, title, exercises FROM lessons WHERE id = ?').get(Number(params[0]));
    if (!row) throw new HttpError(404, 'Không tìm thấy bài học');
    return { lesson: { id: row.id, title: row.title, exercises: JSON.parse(row.exercises) } };
  });

  route('POST', /^\/api\/lessons\/(\d+)\/complete$/, true, async ({ user, params, body }) => {
    const lessonId = Number(params[0]);
    const lesson = db.prepare('SELECT id, exercises FROM lessons WHERE id = ?').get(lessonId);
    if (!lesson) throw new HttpError(404, 'Không tìm thấy bài học');

    const total = Math.max(0, Math.min(200, Math.floor(Number(body.total) || 0)));
    const correct = Math.max(0, Math.min(total, Math.floor(Number(body.correct) || 0)));
    const seconds = Math.max(0, Math.min(36000, Math.floor(Number(body.seconds) || 0)));
    const accuracy = total > 0 ? Math.round((correct / total) * 100) : 100;

    // XP: co ban 30 + 20/cau dung + thuong 50 neu 100%
    const xpGained = 30 + correct * 20 + (accuracy === 100 ? 50 : 0);

    // Streak theo ngay
    const today = todayStr();
    const yesterday = todayStr(new Date(Date.now() - 86400000));
    let streak = user.streak;
    if (user.last_active !== today) streak = user.last_active === yesterday ? user.streak + 1 : 1;

    db.prepare('UPDATE users SET xp = xp + ?, streak = ?, last_active = ? WHERE id = ?').run(
      xpGained, streak, today, user.id
    );

    db.prepare(
      `INSERT INTO progress (user_id, lesson_id, best_accuracy, attempts, completed_at)
       VALUES (?, ?, ?, 1, datetime('now'))
       ON CONFLICT(user_id, lesson_id) DO UPDATE SET
         best_accuracy = MAX(best_accuracy, excluded.best_accuracy),
         attempts = attempts + 1,
         completed_at = datetime('now')`
    ).run(user.id, lessonId, accuracy);

    // Luu tu vung da hoc (cac the "card" trong bai)
    let newWords = 0;
    const addWord = db.prepare(
      'INSERT OR IGNORE INTO user_words (user_id, word, meaning_vi, emoji) VALUES (?, ?, ?, ?)'
    );
    for (const ex of JSON.parse(lesson.exercises)) {
      if (ex.type === 'card') {
        const r = addWord.run(user.id, ex.word.toLowerCase(), ex.meaningVi, ex.emoji || null);
        newWords += Number(r.changes);
      }
    }

    const fresh = getUser(user.id);
    return { xpGained, totalXp: fresh.xp, streak: fresh.streak, accuracy, seconds, newWords };
  });

  // --- Tu vung & bang xep hang ---
  route('GET', /^\/api\/words$/, true, async ({ user }) => ({
    words: db
      .prepare('SELECT word, meaning_vi AS meaningVi, emoji, learned_at AS learnedAt FROM user_words WHERE user_id = ? ORDER BY learned_at DESC, word')
      .all(user.id),
  }));

  route('GET', /^\/api\/leaderboard$/, true, async () => ({
    leaderboard: db.prepare('SELECT name, xp, streak FROM users ORDER BY xp DESC LIMIT 20').all(),
  }));

  route('GET', /^\/health$/, false, async () => ({ ok: true }));

  // --- Dispatcher ---
  return http.createServer(async (req, res) => {
    try {
      if (req.method === 'OPTIONS') return send(res, 204, {});
      const path = new URL(req.url, 'http://localhost').pathname.replace(/\/+$/, '') || '/';

      let matched = null;
      for (const r of routes) {
        if (r.method !== req.method) continue;
        const m = path.match(r.pattern);
        if (m) {
          matched = { r, params: m.slice(1) };
          break;
        }
      }
      if (!matched) throw new HttpError(404, 'Không tìm thấy');

      let user = null;
      if (matched.r.auth) {
        const header = req.headers.authorization || '';
        const payload = header.startsWith('Bearer ') ? verifyToken(header.slice(7)) : null;
        user = payload ? getUser(payload.uid) : null;
        if (!user) throw new HttpError(401, 'Phiên đăng nhập hết hạn');
      }

      const body = req.method === 'POST' ? await readJson(req) : {};
      const out = await matched.r.handler({ req, user, body, params: matched.params });
      if (Array.isArray(out)) send(res, out[0], out[1]);
      else send(res, 200, out);
    } catch (e) {
      if (e instanceof HttpError) return send(res, e.status, { error: e.message });
      console.error(e);
      send(res, 500, { error: 'Lỗi máy chủ' });
    }
  });
}

module.exports = { createServer };

if (require.main === module) {
  const port = Number(process.env.PORT) || 3000;
  const db = openDb(process.env.DB_FILE || 'engapp.db');
  createServer(db).listen(port, '0.0.0.0', () => {
    console.log(`EngApp backend dang chay tai http://0.0.0.0:${port}`);
    if (!process.env.JWT_SECRET) console.warn('Canh bao: chua dat JWT_SECRET, dang dung khoa mac dinh (chi dung khi phat trien).');
  });
}

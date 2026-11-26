import crypto from "node:crypto";
import http from "node:http";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { LESSONS } from "./curriculum.js";
import { JsonStore, publicUser } from "./store.js";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function b64url(input) {
  return Buffer.from(input).toString("base64url");
}

function signToken(payload, secret) {
  const header = b64url(JSON.stringify({ alg: "HS256", typ: "JWT" }));
  const body = b64url(JSON.stringify({ ...payload, exp: Date.now() + 30 * 86_400_000 }));
  const sig = crypto.createHmac("sha256", secret).update(`${header}.${body}`).digest("base64url");
  return `${header}.${body}.${sig}`;
}

function verifyToken(token, secret) {
  const parts = String(token || "").split(".");
  if (parts.length !== 3) return null;
  const [header, body, sig] = parts;
  const expected = crypto.createHmac("sha256", secret).update(`${header}.${body}`).digest("base64url");
  if (sig.length !== expected.length || !crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(expected))) {
    return null;
  }
  try {
    const payload = JSON.parse(Buffer.from(body, "base64url").toString("utf8"));
    if (!payload.exp || payload.exp < Date.now()) return null;
    return payload;
  } catch {
    return null;
  }
}

function readJsonBody(req) {
  return new Promise((resolve, reject) => {
    let raw = "";
    req.on("data", (chunk) => {
      raw += chunk;
      if (raw.length > 1_000_000) {
        reject(new Error("Payload too large"));
        req.destroy();
      }
    });
    req.on("end", () => {
      if (!raw.trim()) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch {
        reject(new Error("INVALID_JSON"));
      }
    });
    req.on("error", reject);
  });
}

function sendJson(res, status, data) {
  const payload = JSON.stringify(data);
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  });
  res.end(payload);
}

/**
 * Tạo HTTP server tách biệt hoàn toàn với client Android.
 */
export function createAppServer({
  dataFile = process.env.DATA_FILE || path.join(__dirname, "..", "data", "db.json"),
  jwtSecret = process.env.JWT_SECRET || "english-cat-dev-secret-change-in-prod",
} = {}) {
  const store = new JsonStore(dataFile);

  function authUser(req) {
    const header = req.headers.authorization || "";
    const token = header.startsWith("Bearer ") ? header.slice(7).trim() : "";
    const claims = verifyToken(token, jwtSecret);
    if (!claims) return null;
    return store.findById(claims.sub);
  }

  return http.createServer(async (req, res) => {
    if (req.method === "OPTIONS") {
      return sendJson(res, 204, {});
    }

    const url = new URL(req.url || "/", "http://localhost");
    const pathname = url.pathname;

    try {
      if (req.method === "GET" && pathname === "/health") {
        return sendJson(res, 200, { ok: true });
      }

      if (req.method === "POST" && pathname === "/api/auth/register") {
        const { name = "", email = "", password = "" } = await readJsonBody(req);
        if (!name.trim()) return sendJson(res, 400, { error: "Vui lòng nhập tên." });
        if (!email.includes("@")) return sendJson(res, 400, { error: "Email không hợp lệ." });
        if (String(password).length < 6) return sendJson(res, 400, { error: "Mật khẩu tối thiểu 6 ký tự." });
        if (store.findByEmail(email)) return sendJson(res, 409, { error: "Email đã được sử dụng." });

        const user = store.createUser({ name, email, password });
        const token = signToken({ sub: user.id }, jwtSecret);
        return sendJson(res, 201, { user: publicUser(user), token });
      }

      if (req.method === "POST" && pathname === "/api/auth/login") {
        const { email = "", password = "" } = await readJsonBody(req);
        const user = store.findByEmail(email);
        if (!user || !store.verifyPassword(user, String(password))) {
          return sendJson(res, 401, { error: "Email hoặc mật khẩu không đúng." });
        }
        const token = signToken({ sub: user.id }, jwtSecret);
        return sendJson(res, 200, { user: publicUser(user), token });
      }

      const user = authUser(req);
      if (!user) {
        return sendJson(res, 401, { error: "Phiên đăng nhập hết hạn" });
      }

      if (req.method === "GET" && pathname === "/api/me") {
        return sendJson(res, 200, { user: publicUser(user) });
      }

      if (req.method === "GET" && pathname === "/api/lessons") {
        const lessons = LESSONS.map((l) => {
          const prog = user.progress[String(l.id)] || { completed: false, bestAccuracy: 0 };
          return {
            id: l.id,
            title: l.title,
            chapterTitle: l.chapterTitle,
            wordCount: l.wordCount,
            totalExercises: l.exercises.length,
            completed: Boolean(prog.completed),
            bestAccuracy: prog.bestAccuracy || 0,
          };
        });
        return sendJson(res, 200, { lessons });
      }

      const detailMatch = pathname.match(/^\/api\/lessons\/(\d+)$/);
      if (req.method === "GET" && detailMatch) {
        const id = Number(detailMatch[1]);
        const lesson = LESSONS.find((l) => l.id === id);
        if (!lesson) return sendJson(res, 404, { error: "Không tìm thấy bài học." });
        return sendJson(res, 200, {
          lesson: {
            id: lesson.id,
            title: lesson.title,
            exercises: lesson.exercises,
          },
        });
      }

      const completeMatch = pathname.match(/^\/api\/lessons\/(\d+)\/complete$/);
      if (req.method === "POST" && completeMatch) {
        const id = Number(completeMatch[1]);
        const lesson = LESSONS.find((l) => l.id === id);
        if (!lesson) return sendJson(res, 404, { error: "Không tìm thấy bài học." });

        const body = await readJsonBody(req);
        const result = store.recordLessonCompletion(user.id, lesson, body);
        return sendJson(res, 200, result);
      }

      return sendJson(res, 404, { error: "Không tìm thấy endpoint." });
    } catch (err) {
      if (err?.message === "INVALID_JSON") {
        return sendJson(res, 400, { error: "Dữ liệu JSON không hợp lệ." });
      }
      return sendJson(res, 500, { error: "Lỗi máy chủ nội bộ." });
    }
  });
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(fileURLToPath(import.meta.url))) {
  const port = Number(process.env.PORT || 3000);
  const server = createAppServer();
  server.listen(port, () => {
    console.log(`English Cat backend listening on http://localhost:${port}`);
  });
}

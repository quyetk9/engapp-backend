import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { createAppServer } from "../src/server.js";

const tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), "engcat-test-"));
const dataFile = path.join(tmpDir, "db.json");
const server = createAppServer({ dataFile, jwtSecret: "test-secret" });

await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
const { port } = server.address();
const base = `http://127.0.0.1:${port}`;

async function call(method, p, body, token) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  const res = await fetch(`${base}${p}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });
  return { status: res.status, json: await res.json() };
}

try {
  // 1. Health
  const h = await call("GET", "/health");
  assert.equal(h.status, 200);
  assert.equal(h.json.ok, true);

  // 2. Unauthorized lessons check
  const unauth = await call("GET", "/api/lessons");
  assert.equal(unauth.status, 401);

  // 3. Register + duplicate rejection
  const reg = await call("POST", "/api/auth/register", {
    name: "Linh",
    email: "linh@example.com",
    password: "secret123",
  });
  assert.equal(reg.status, 201);
  assert.ok(reg.json.token);
  assert.equal(reg.json.user.name, "Linh");

  const dup = await call("POST", "/api/auth/register", {
    name: "Linh",
    email: "linh@example.com",
    password: "secret123",
  });
  assert.equal(dup.status, 409);

  // 4. Login
  const login = await call("POST", "/api/auth/login", {
    email: "linh@example.com",
    password: "secret123",
  });
  assert.equal(login.status, 200);
  const token = login.json.token;

  // 5. Lessons list & detail
  const lessons = await call("GET", "/api/lessons", null, token);
  assert.equal(lessons.status, 200);
  assert.ok(lessons.json.lessons.length >= 5);
  assert.equal(lessons.json.lessons[0].chapterTitle, "Chào hỏi và giao tiếp cơ bản");

  const detail = await call("GET", "/api/lessons/1", null, token);
  assert.equal(detail.status, 200);
  assert.ok(detail.json.lesson.exercises.some((e) => e.type === "tip"));
  assert.ok(detail.json.lesson.exercises.some((e) => e.type === "true_false"));

  // 6. Complete lesson & verify XP/streak/progress
  const complete = await call("POST", "/api/lessons/1/complete", {
    correct: 6,
    total: 6,
    seconds: 45,
  }, token);
  assert.equal(complete.status, 200);
  assert.equal(complete.json.accuracy, 100);
  assert.equal(complete.json.streak, 1);
  assert.ok(complete.json.xpGained > 0);
  assert.equal(complete.json.newWords, 4);

  const me = await call("GET", "/api/me", null, token);
  assert.equal(me.status, 200);
  assert.equal(me.json.user.xp, complete.json.totalXp);

  console.log("ALL BACKEND TESTS PASSED");
} finally {
  server.close();
  fs.rmSync(tmpDir, { recursive: true, force: true });
}

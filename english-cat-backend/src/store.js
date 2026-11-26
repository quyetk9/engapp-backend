import crypto from "node:crypto";
import fs from "node:fs";
import path from "node:path";

/**
 * Lưu trữ dữ liệu người dùng & tiến độ học tập bằng file JSON (zero-dependency).
 */
export class JsonStore {
  constructor(dataFile) {
    this.dataFile = dataFile;
    this.db = this.#load();
  }

  #load() {
    try {
      if (fs.existsSync(this.dataFile)) {
        const parsed = JSON.parse(fs.readFileSync(this.dataFile, "utf8"));
        if (parsed && Array.isArray(parsed.users)) {
          return { nextId: parsed.nextId || parsed.users.length + 1, users: parsed.users };
        }
      }
    } catch {
      // Tạo mới nếu file hỏng hoặc rỗng
    }
    return { nextId: 1, users: [] };
  }

  #save() {
    const dir = path.dirname(this.dataFile);
    fs.mkdirSync(dir, { recursive: true });
    const tmp = `${this.dataFile}.tmp`;
    fs.writeFileSync(tmp, JSON.stringify(this.db, null, 2), "utf8");
    fs.renameSync(tmp, this.dataFile);
  }

  findByEmail(email) {
    const norm = email.trim().toLowerCase();
    return this.db.users.find((u) => u.email.toLowerCase() === norm) || null;
  }

  findById(id) {
    return this.db.users.find((u) => u.id === id) || null;
  }

  createUser({ name, email, password }) {
    const salt = crypto.randomBytes(16).toString("hex");
    const hash = crypto.scryptSync(password, salt, 64).toString("hex");
    const user = {
      id: this.db.nextId++,
      name: name.trim(),
      email: email.trim().toLowerCase(),
      passwordHash: `${salt}:${hash}`,
      xp: 0,
      streak: 0,
      lastCompletedDate: null,
      progress: {},
    };
    this.db.users.push(user);
    this.#save();
    return user;
  }

  verifyPassword(user, password) {
    const [salt, expectedHex] = (user.passwordHash || "").split(":");
    if (!salt || !expectedHex) return false;
    const actual = crypto.scryptSync(password, salt, 64);
    const expected = Buffer.from(expectedHex, "hex");
    return actual.length === expected.length && crypto.timingSafeEqual(actual, expected);
  }

  recordLessonCompletion(userId, lesson, { correct, total, seconds }) {
    const user = this.findById(userId);
    if (!user) return null;

    const safeTotal = Math.max(1, Number(total) || 1);
    const safeCorrect = Math.max(0, Math.min(safeTotal, Number(correct) || 0));
    const safeSeconds = Math.max(1, Number(seconds) || 1);
    const accuracy = Math.round((safeCorrect * 100) / safeTotal);
    const xpGained = 10 + safeCorrect * 5 + (accuracy === 100 ? 10 : 0);

    const today = new Date().toISOString().slice(0, 10);
    const yesterday = new Date(Date.now() - 86_400_000).toISOString().slice(0, 10);
    if (user.lastCompletedDate === today) {
      user.streak = Math.max(1, user.streak);
    } else if (user.lastCompletedDate === yesterday) {
      user.streak += 1;
    } else {
      user.streak = 1;
    }
    user.lastCompletedDate = today;
    user.xp += xpGained;

    const key = String(lesson.id);
    const prev = user.progress[key] || { completed: false, bestAccuracy: 0 };
    const isFirstComplete = !prev.completed;
    user.progress[key] = {
      completed: true,
      bestAccuracy: Math.max(prev.bestAccuracy || 0, accuracy),
    };
    this.#save();

    return {
      xpGained,
      totalXp: user.xp,
      streak: user.streak,
      accuracy,
      seconds: safeSeconds,
      newWords: isFirstComplete ? lesson.wordCount || 0 : 0,
    };
  }
}

export function publicUser(user) {
  return {
    id: user.id,
    name: user.name,
    email: user.email,
    xp: user.xp,
    streak: user.streak,
  };
}

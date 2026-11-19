'use strict';
const { DatabaseSync } = require('node:sqlite');
const { LESSONS } = require('./data');

function openDb(file = 'engapp.db') {
  const db = new DatabaseSync(file);
  db.exec(`
    PRAGMA foreign_keys = ON;

    CREATE TABLE IF NOT EXISTS users (
      id          INTEGER PRIMARY KEY AUTOINCREMENT,
      name        TEXT NOT NULL,
      email       TEXT NOT NULL UNIQUE,
      pass_hash   TEXT NOT NULL,
      xp          INTEGER NOT NULL DEFAULT 0,
      streak      INTEGER NOT NULL DEFAULT 0,
      last_active TEXT,
      created_at  TEXT NOT NULL DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS lessons (
      id        INTEGER PRIMARY KEY,
      position  INTEGER NOT NULL,
      title     TEXT NOT NULL,
      exercises TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS progress (
      user_id       INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      lesson_id     INTEGER NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
      best_accuracy INTEGER NOT NULL DEFAULT 0,
      attempts      INTEGER NOT NULL DEFAULT 0,
      completed_at  TEXT,
      PRIMARY KEY (user_id, lesson_id)
    );

    CREATE TABLE IF NOT EXISTS user_words (
      user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      word       TEXT NOT NULL,
      meaning_vi TEXT NOT NULL,
      emoji      TEXT,
      learned_at TEXT NOT NULL DEFAULT (datetime('now')),
      PRIMARY KEY (user_id, word)
    );
  `);

  // Dong bo noi dung bai hoc tu data.js moi lan khoi dong
  const upsert = db.prepare(
    'INSERT OR REPLACE INTO lessons (id, position, title, exercises) VALUES (?, ?, ?, ?)'
  );
  LESSONS.forEach((l, i) => upsert.run(l.id, i + 1, l.title, JSON.stringify(l.exercises)));

  return db;
}

module.exports = { openDb };

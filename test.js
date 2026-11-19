'use strict';
// Chay: npm test  (khong can cai them thu vien)
const assert = require('node:assert/strict');
const { openDb } = require('./db');
const { createServer } = require('./server');

(async () => {
  const server = createServer(openDb(':memory:'));
  await new Promise((r) => server.listen(0, r));
  const base = `http://127.0.0.1:${server.address().port}`;

  const call = async (method, path, body, token) => {
    const res = await fetch(base + path, {
      method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
    return { status: res.status, data: await res.json() };
  };

  let r = await call('GET', '/health');
  assert.equal(r.status, 200);

  r = await call('GET', '/api/lessons');
  assert.equal(r.status, 401, 'can dang nhap de xem bai hoc');

  r = await call('POST', '/api/auth/register', { name: 'An', email: 'an@example.com', password: '123' });
  assert.equal(r.status, 400, 'mat khau ngan bi tu choi');

  r = await call('POST', '/api/auth/register', { name: 'An', email: 'an@example.com', password: '123456' });
  assert.equal(r.status, 201);
  const token = r.data.token;

  r = await call('POST', '/api/auth/register', { name: 'An', email: 'an@example.com', password: '123456' });
  assert.equal(r.status, 409, 'email trung');

  r = await call('POST', '/api/auth/login', { email: 'an@example.com', password: 'sai-mat-khau' });
  assert.equal(r.status, 401);

  r = await call('POST', '/api/auth/login', { email: 'AN@example.com', password: '123456' });
  assert.equal(r.status, 200);

  r = await call('GET', '/api/lessons', null, token);
  assert.equal(r.status, 200);
  assert.equal(r.data.lessons.length, 3);
  assert.equal(r.data.lessons[0].completed, false);

  r = await call('GET', '/api/lessons/1', null, token);
  assert.equal(r.status, 200);
  const types = new Set(r.data.lesson.exercises.map((e) => e.type));
  for (const t of ['card', 'match', 'fill', 'listen', 'order']) assert.ok(types.has(t), `thieu dang bai ${t}`);

  // Kiem tra tinh hop le cua du lieu bai tap
  for (const id of [1, 2, 3]) {
    const { data } = await call('GET', `/api/lessons/${id}`, null, token);
    for (const ex of data.lesson.exercises) {
      if (ex.type === 'fill') {
        assert.equal(ex.template.split('___').length - 1, ex.answers.length, `so cho trong != so dap an (bai ${id})`);
        for (const a of ex.answers) assert.ok(ex.options.includes(a), `dap an "${a}" khong co trong options (bai ${id})`);
      }
      if (ex.type === 'order') for (const w of ex.answer) assert.ok(ex.words.includes(w));
      if (ex.type === 'listen') assert.ok(ex.answerIndex >= 0 && ex.answerIndex < ex.options.length);
      if (ex.type === 'match') assert.equal(new Set(ex.pairs.map((p) => p.word)).size, ex.pairs.length);
    }
  }

  r = await call('GET', '/api/lessons/99', null, token);
  assert.equal(r.status, 404);

  r = await call('POST', '/api/lessons/1/complete', { correct: 10, total: 11, seconds: 188 }, token);
  assert.equal(r.status, 200);
  assert.equal(r.data.accuracy, 91);
  assert.equal(r.data.xpGained, 30 + 10 * 20);
  assert.equal(r.data.streak, 1);
  assert.equal(r.data.newWords, 3);

  r = await call('POST', '/api/lessons/1/complete', { correct: 11, total: 11, seconds: 100 }, token);
  assert.equal(r.data.streak, 1, 'cung ngay khong tang streak');
  assert.equal(r.data.newWords, 0, 'tu da hoc khong dem lai');

  r = await call('GET', '/api/lessons', null, token);
  assert.equal(r.data.lessons[0].completed, true);
  assert.equal(r.data.lessons[0].bestAccuracy, 100);

  r = await call('GET', '/api/words', null, token);
  assert.equal(r.data.words.length, 3);

  r = await call('GET', '/api/me', null, token);
  assert.equal(r.data.user.xp, 230 + 300);

  r = await call('GET', '/api/leaderboard', null, token);
  assert.equal(r.data.leaderboard[0].name, 'An');

  server.close();
  console.log('Tat ca bai kiem tra backend deu dat.');
})().catch((e) => {
  console.error('LOI:', e);
  process.exit(1);
});

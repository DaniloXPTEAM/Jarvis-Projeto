// scripts/migrate.js
// Cria as tabelas no PostgreSQL (lê DATABASE_URL do ambiente).
// Idempotente (CREATE TABLE IF NOT EXISTS) — roda a cada build do Render, sem fazer mal.
// Usado porque o plano grátis do Render não libera o Shell.
const { Client } = require('pg');

const DDL = [
  `CREATE TABLE IF NOT EXISTS memories (
    id text PRIMARY KEY,
    type text NOT NULL,
    title text NOT NULL,
    content text NOT NULL,
    tags text DEFAULT '',
    done boolean DEFAULT false,
    due_at timestamp,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
  )`,
  `CREATE TABLE IF NOT EXISTS skill_logs (
    id text PRIMARY KEY,
    skill_name text NOT NULL,
    input text NOT NULL,
    output text NOT NULL,
    success boolean DEFAULT true,
    executed_at timestamp NOT NULL DEFAULT now()
  )`,
  `CREATE TABLE IF NOT EXISTS chat_messages (
    id text PRIMARY KEY,
    role text NOT NULL,
    content text NOT NULL,
    skill_routed text,
    created_at timestamp NOT NULL DEFAULT now()
  )`,
  `CREATE TABLE IF NOT EXISTS devices (
    id text PRIMARY KEY,
    name text NOT NULL,
    type text NOT NULL,
    last_seen timestamp DEFAULT now(),
    trusted boolean DEFAULT false
  )`,
];

(async () => {
  if (!process.env.DATABASE_URL) {
    console.warn('migrate: DATABASE_URL não definida — pulando.');
    return;
  }
  const client = new Client({ connectionString: process.env.DATABASE_URL });
  try {
    await client.connect();
    for (const sql of DDL) await client.query(sql);
    console.log('migrate: tabelas verificadas/criadas com sucesso ✅');
  } catch (e) {
    console.error('migrate: erro ->', e.message);
    process.exit(1);
  } finally {
    await client.end();
  }
})();

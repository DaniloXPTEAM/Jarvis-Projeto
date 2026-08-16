import { sqliteTable, text, integer, real } from 'drizzle-orm/sqlite-core';

// VAULT: tudo que entra vira memória
export const memories = sqliteTable('memories', {
  id: text('id').primaryKey(),
  type: text('type').notNull(), // 'reminder' | 'note' | 'idea' | 'log' | 'skill_output'
  title: text('title').notNull(),
  content: text('content').notNull(),
  tags: text('tags').default(''), // comma-separated
  done: integer('done', { mode: 'boolean' }).default(false),
  dueAt: integer('due_at', { mode: 'timestamp' }),
  createdAt: integer('created_at', { mode: 'timestamp' }).default(new Date()).notNull(),
  updatedAt: integer('updated_at', { mode: 'timestamp' }).default(new Date()).notNull(),
});

// SKILL LOGS: histórico de skills executadas
export const skillLogs = sqliteTable('skill_logs', {
  id: text('id').primaryKey(),
  skillName: text('skill_name').notNull(),
  input: text('input').notNull(),
  output: text('output').notNull(),
  success: integer('success', { mode: 'boolean' }).default(true),
  executedAt: integer('executed_at', { mode: 'timestamp' }).default(new Date()).notNull(),
});

// CHAT HISTORY: conversas com a IA
export const chatMessages = sqliteTable('chat_messages', {
  id: text('id').primaryKey(),
  role: text('role').notNull(), // 'user' | 'assistant' | 'system'
  content: text('content').notNull(),
  skillRouted: text('skill_routed'), // qual skill foi ativada
  createdAt: integer('created_at', { mode: 'timestamp' }).default(new Date()).notNull(),
});

// BLUETOOTH DEVICES: dispositivos conhecidos
export const devices = sqliteTable('devices', {
  id: text('id').primaryKey(),
  name: text('name').notNull(),
  type: text('type').notNull(), // 'speaker' | 'headphones' | 'alexa' | 'other'
  lastSeen: integer('last_seen', { mode: 'timestamp' }).default(new Date()),
  trusted: integer('trusted', { mode: 'boolean' }).default(false),
});
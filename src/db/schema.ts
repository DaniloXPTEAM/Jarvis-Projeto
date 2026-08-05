import { pgTable, text, timestamp, boolean, integer } from 'drizzle-orm/pg-core';

// VAULT: tudo que entra vira memória
export const memories = pgTable('memories', {
  id: text('id').primaryKey(),
  type: text('type').notNull(), // 'reminder' | 'note' | 'idea' | 'log' | 'skill_output'
  title: text('title').notNull(),
  content: text('content').notNull(),
  tags: text('tags').default(''), // comma-separated
  done: boolean('done').default(false),
  dueAt: timestamp('due_at'),
  createdAt: timestamp('created_at').defaultNow().notNull(),
  updatedAt: timestamp('updated_at').defaultNow().notNull(),
});

// SKILL LOGS: histórico de skills executadas
export const skillLogs = pgTable('skill_logs', {
  id: text('id').primaryKey(),
  skillName: text('skill_name').notNull(),
  input: text('input').notNull(),
  output: text('output').notNull(),
  success: boolean('success').default(true),
  executedAt: timestamp('executed_at').defaultNow().notNull(),
});

// CHAT HISTORY: conversas com a IA
export const chatMessages = pgTable('chat_messages', {
  id: text('id').primaryKey(),
  role: text('role').notNull(), // 'user' | 'assistant' | 'system'
  content: text('content').notNull(),
  skillRouted: text('skill_routed'), // qual skill foi ativada
  createdAt: timestamp('created_at').defaultNow().notNull(),
});

// BLUETOOTH DEVICES: dispositivos conhecidos
export const devices = pgTable('devices', {
  id: text('id').primaryKey(),
  name: text('name').notNull(),
  type: text('type').notNull(), // 'speaker' | 'headphones' | 'alexa' | 'other'
  lastSeen: timestamp('last_seen').defaultNow(),
  trusted: boolean('trusted').default(false),
});

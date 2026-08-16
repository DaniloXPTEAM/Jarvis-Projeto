import { drizzle } from 'drizzle-orm/better-sqlite3';
import Database from 'better-sqlite3';

const databaseUrl = process.env.DATABASE_URL || 'file:./data/app.db';
const filePath = databaseUrl.replace('file:', '');

const globalForDb = globalThis as typeof globalThis & {
  __sqliteDb?: Database.Database;
};

export const sqlite =
  globalForDb.__sqliteDb ??
  new Database(filePath, {
    // WAL mode para melhor concorrência
    // fileMustExist: false
  });

sqlite.pragma('journal_mode = WAL');
sqlite.pragma('busy_timeout = 5000');

if (process.env.NODE_ENV !== 'production') {
  globalForDb.__sqliteDb = sqlite;
}

export const db = drizzle(sqlite);
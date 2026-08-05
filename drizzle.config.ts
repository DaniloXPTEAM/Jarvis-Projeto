import { defineConfig } from 'drizzle-kit';

// Lê a URL do banco do ambiente (Render injeta DATABASE_URL).
// Localmente, use .env.local com DATABASE_URL ou cai no padrão abaixo.
export default defineConfig({
  dialect: 'postgresql',
  schema: './src/db/schema.ts',
  dbCredentials: {
    url: process.env.DATABASE_URL || 'postgresql://postgres:postgres@127.0.0.1:5432/app_db',
  },
});

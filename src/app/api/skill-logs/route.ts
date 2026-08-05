import { NextRequest, NextResponse } from 'next/server';
import { db } from '@/db';
import { skillLogs } from '@/db/schema';
import { desc } from 'drizzle-orm';
import { randomUUID } from 'crypto';

export async function GET() {
  try {
    const rows = await db.select().from(skillLogs).orderBy(desc(skillLogs.executedAt)).limit(50);
    return NextResponse.json(rows);
  } catch {
    return NextResponse.json([]);
  }
}

export async function POST(req: NextRequest) {
  const body = await req.json();
  const row = {
    id:          randomUUID(),
    skillName:   body.skillName  ?? 'unknown',
    input:       body.input      ?? '',
    output:      body.output     ?? '',
    success:     body.success    ?? true,
    executedAt:  new Date(),
  };
  await db.insert(skillLogs).values(row);
  return NextResponse.json(row, { status: 201 });
}

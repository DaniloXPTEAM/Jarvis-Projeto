import { NextRequest, NextResponse } from 'next/server';
import { db } from '@/db';
import { memories } from '@/db/schema';
import { eq, desc } from 'drizzle-orm';
import { randomUUID } from 'crypto';

export async function GET(req: NextRequest) {
  const type = new URL(req.url).searchParams.get('type');
  try {
    const rows = type
      ? await db.select().from(memories).where(eq(memories.type, type)).orderBy(desc(memories.createdAt))
      : await db.select().from(memories).orderBy(desc(memories.createdAt));
    return NextResponse.json(rows);
  } catch { return NextResponse.json([]); }
}

export async function POST(req: NextRequest) {
  const b = await req.json();
  const now = new Date();
  const row = {
    id: randomUUID(), type: b.type ?? 'note',
    title: b.title ?? 'Sem título', content: b.content ?? '',
    tags: b.tags ?? '', done: false,
    dueAt: b.dueAt ? new Date(b.dueAt) : null,
    createdAt: now, updatedAt: now,
  };
  await db.insert(memories).values(row);
  return NextResponse.json(row, { status: 201 });
}

export async function PATCH(req: NextRequest) {
  const { id, ...updates } = await req.json();
  if (!id) return NextResponse.json({ error: 'id required' }, { status: 400 });
  await db.update(memories).set({ ...updates, updatedAt: new Date() }).where(eq(memories.id, id));
  return NextResponse.json({ ok: true });
}

export async function DELETE(req: NextRequest) {
  const id = new URL(req.url).searchParams.get('id');
  if (!id) return NextResponse.json({ error: 'id required' }, { status: 400 });
  await db.delete(memories).where(eq(memories.id, id));
  return NextResponse.json({ ok: true });
}

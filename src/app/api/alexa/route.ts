import { NextRequest, NextResponse } from 'next/server';

/**
 * ALEXA INTEGRATION
 *
 * Controla dispositivos Echo via API interna da Amazon (cookie auth).
 *
 * Para configurar:
 *   1. npm install -g alexa-cookie-cli
 *   2. alexa-cookie-cli  →  faça login na Amazon
 *   3. Copie o cookie gerado
 *   4. Adicione ALEXA_COOKIE nas variáveis de ambiente
 *   5. Opcionalmente adicione ALEXA_CSRF
 *
 * A lib alexa-remote2 é usada como referência de API,
 * aqui reimplementada com fetch nativo para compatibilidade.
 */

const ALEXA_HOST = 'https://alexa.amazon.com.br';

function makeHeaders() {
  const cookie = process.env.ALEXA_COOKIE || '';
  const csrf   = process.env.ALEXA_CSRF   || '0';
  return {
    'Content-Type': 'application/json; charset=UTF-8',
    'Cookie':       cookie,
    'csrf':         csrf,
    'DNT':          '1',
    'Referer':      'https://alexa.amazon.com.br/spa/index.html',
    'Origin':       'https://alexa.amazon.com.br',
    'User-Agent':   'Mozilla/5.0 (compatible; JARVIS-OS/1.0)',
  };
}

async function listDevices() {
  const res = await fetch(`${ALEXA_HOST}/api/devices-v2/device?raw=false`, {
    headers: makeHeaders(),
  });
  if (!res.ok) throw new Error(`Amazon API ${res.status}`);
  const d = await res.json();
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return (d.devices || []).map((dev: any) => ({
    id:     dev.serialNumber,
    name:   dev.accountName || dev.accountName,
    type:   dev.deviceType,
    online: dev.online ?? true,
    family: dev.deviceFamily,
  }));
}

async function sendAnnounce(text: string, deviceId?: string) {
  // Alexa behaviors API — speak
  const payload = {
    behaviorId:   'PREVIEW',
    sequenceJson: JSON.stringify({
      '@type': 'com.amazon.alexa.behaviors.model.Sequence',
      startNode: {
        '@type': 'com.amazon.alexa.behaviors.model.OrchestratorNode',
        nodesToExecute: [{
          '@type': 'com.amazon.alexa.behaviors.model.SerialNode',
          nodesToExecute: [{
            '@type': 'com.amazon.alexa.behaviors.model.EndpointNode',
            operationPayload: {
              deviceType:            deviceId ? 'SERIAL' : 'ALEXA_ALL_DSN',
              deviceSerialNumber:    deviceId || 'ALEXA_ALL_DSN',
              locale:                'pt-BR',
              customerId:            '',
              textToSpeak:           text,
            },
            type: 'Alexa.TextCommand',
          }],
        }],
      },
    }),
    status: 'ENABLED',
  };

  const res = await fetch(`${ALEXA_HOST}/api/behaviors/preview`, {
    method: 'POST',
    headers: makeHeaders(),
    body: JSON.stringify(payload),
  });
  return res.ok;
}

async function setVolume(volume: number, deviceId?: string) {
  const res = await fetch(`${ALEXA_HOST}/api/behaviors/preview`, {
    method: 'POST',
    headers: makeHeaders(),
    body: JSON.stringify({
      behaviorId: 'PREVIEW',
      sequenceJson: JSON.stringify({
        '@type': 'com.amazon.alexa.behaviors.model.Sequence',
        startNode: {
          '@type': 'com.amazon.alexa.behaviors.model.OrchestratorNode',
          nodesToExecute: [{
            '@type': 'com.amazon.alexa.behaviors.model.SerialNode',
            nodesToExecute: [{
              '@type': 'com.amazon.alexa.behaviors.model.EndpointNode',
              operationPayload: {
                deviceType:         deviceId ? 'SERIAL' : 'ALEXA_ALL_DSN',
                deviceSerialNumber: deviceId || 'ALEXA_ALL_DSN',
                customerId:         '',
                value:              volume,
              },
              type: 'Alexa.DeviceControls.Volume',
            }],
          }],
        },
      }),
      status: 'ENABLED',
    }),
  });
  return res.ok;
}

/* ── GET /api/alexa ─ lista dispositivos ──────────────────────── */
export async function GET() {
  const hasKey = !!process.env.ALEXA_COOKIE;
  if (!hasKey) {
    return NextResponse.json({
      connected: false,
      devices:   [],
      message:   'Configure ALEXA_COOKIE para conectar dispositivos reais.',
    });
  }

  try {
    const devices = await listDevices();
    return NextResponse.json({ connected: true, devices });
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Erro desconhecido';
    return NextResponse.json({ connected: false, devices: [], error: msg });
  }
}

/* ── POST /api/alexa ─ envia comando ─────────────────────────── */
export async function POST(req: NextRequest) {
  const body = await req.json() as {
    action: 'announce' | 'speak' | 'volume';
    deviceId?: string;
    text?: string;
    volume?: number;
  };

  const hasKey = !!process.env.ALEXA_COOKIE;

  if (!hasKey) {
    // Simulated response
    return NextResponse.json({
      ok:        true,
      simulated: true,
      action:    body.action,
      text:      body.text,
      message:   `[SIMULADO] ${body.action}: ${body.text || body.volume}`,
    });
  }

  try {
    let ok = false;
    if (body.action === 'announce' || body.action === 'speak') {
      ok = await sendAnnounce(body.text || 'Olá!', body.deviceId);
    } else if (body.action === 'volume') {
      ok = await setVolume(body.volume ?? 50, body.deviceId);
    }
    return NextResponse.json({ ok, simulated: false });
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Erro';
    return NextResponse.json({ ok: false, error: msg }, { status: 500 });
  }
}

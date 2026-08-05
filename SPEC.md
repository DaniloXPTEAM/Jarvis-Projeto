# SecretaryAI - Sua Secretária Particular Inteligente

## 1. Concept & Vision

**SecretaryAI** é um aplicativo mobile que transforma seu celular em umasecretária pessoal24/7. Conecta-se via Bluetooth a dispositivos como Alexa, caixas de som inteligentes ou fones de ouvido para receber comandos de voz e responder de forma natural. O app é seu companheiro para organização pessoal, respostas rápidas, sugestões inteligentes e integração com IA avançada (Claude) para transformar suas ideias em realidade.

A sensação é de ter uma **assistente pessoal prestativa** que nunca dorme, sempre tem uma sugestãopronta e ajuda a materializar suas ideias.

## 2. Design Language

### Aesthetic Direction
Visual **"Glass Intelligence"** - interfaces com efeito glassmorphism escuro com toques de luz neon, transmitindo alta tecnologia e sofisticação. Inspirado em interfaces de ficção científica futuristas mas acessíveis.

### Color Palette
- **Primary**: `#8B5CF6` (Violet - inteligência e criatividade)
- **Secondary**: `#06B6D4` (Cyan - tecnologia e conectividade)
- **Accent**: `#F59E0B` (Amber - alertas e destaques)
- **Background**: `#0F0F1A` (Deep Space)
- **Surface**: `rgba(139, 92, 246, 0.1)` (Glass violet)
- **Text Primary**: `#F8FAFC`
- **Text Secondary**: `#94A3B8`
- **Success**: `#10B981`
- **Error**: `#EF4444`

### Typography
- **Headings**: `Inter` (700, 600) - moderna e legível
- **Body**: `Inter` (400, 500) - consistência
- **Mono/Labels**: `JetBrains Mono` - dados e códigos

### Spatial System
- Base unit: 4px
- Touch targets: mínimo 48px
- Card padding: 16px-20px
- Section gaps: 24px
- Border radius: 12px-16px

### Motion Philosophy
- **Entrada**: Fade-in + slide-up, 300ms ease-out
- **Interações**: Scale 0.95→1 no tap, 150ms
- **Loading**: Pulse com gradiente violeta
- **Microfone ativo**: Ondas sonar animadas em cyan
- **Transições de tela**: Slide horizontal 250ms

### Visual Assets
- **Icons**: Lucide React (outline style, 24px)
- **Ilustrações**: Gradientes violet→cyan abstratos
- **Decorativos**: Círculos blur de luz, linhas de grade sutis

## 3. Layout & Structure

### Estrutura de Navegação
```
┌─────────────────────────────┐
│  Status Bar (sistema)       │
├─────────────────────────────┤
│  Header com saudação       │
│  + status Bluetooth         │
├─────────────────────────────┤
│                             │
│  Área Principal (scroll)    │
│  - Cards de funcionalidades │
│  - Chat/conversa            │
│                             │
├─────────────────────────────┤
│  Quick Actions (atalhos)    │
├─────────────────────────────┤
│  Input de voz/comando       │
│  [ 🎤 Mic ] [ 💬 Chat ]    │
└─────────────────────────────┘
```

### Telas Principais
1. **Home** - Dashboard com cards de funcionalidades
2. **Chat** - Conversação por texto com IA
3. **Lembretes** - Lista de lembretes e eventos
4. **Favoritos** - Atalhos e snippets salvos
5. **Configurações** - Bluetooth, preferências

### Responsive Strategy
- Mobile-first (320px-428px como base)
- Touch targets generosos (48px+)
- Scroll suave com momentum
- Safe areas para notch/home indicator

## 4. Features & Interactions

### Core Features

#### 🎙️ Assistente de Voz
- **Conexão Bluetooth**: Escaneia e conecta a dispositivos compatíveis
- **Escuta ativa**: Mostra ondas sonar quando aguardando comando
- **Transcrição**: Converte voz em texto em tempo real
- **Resposta por voz**: Fala respostas usando TTS do dispositivo

#### 📅 Gerenciamento de Datas
- Criar lembretes por voz: "Lembra eu de... amanhã às 9h"
- Lista de eventos com countdown
- Notificações programadas
- Visualização em calendário

#### ⚡ Atalhos Rápidos (Kembretes)
- Criar comandos personalizados: "casa" → "Estou em casa"
- Executar ações com um toque
- Compartilhar عبر NFC/Bluetooth
- Categorias: Trabalho, Pessoal, Emergência

#### 🍕 Sugestões de Comida
- "O que eu como agora?"
- Considera horário do dia, clima, preferências
- Integração com APIs de delivery (mock)
- Receitas rápidas caseiras

#### 📰 Atualidades
- Resumo de notícias do dia
- Categorias: Tecnologia, Esportes, Economia, Entretenimento
- Busca por temas específicos

#### 🤖 Integração Claude
- Analisa ideias e sugere implementações
- Gera código, fluxos, planejamentos
-brainstorming interativo
- Salvar ideias como projetos

### Interações Detalhadas

**Microfone (pressionar e soltar)**
- Touch down: Vibração háptica, ícone pulsa violet
- Escuta: Ondas sonar cyan animadas
- Processando: Spinner violeta
- Resposta: Card aparece com animação slide-up

**Card de Lemrete**
- Tap: Expande para ver detalhes
- Swipe left: Marcar como feito (verde)
- Swipe right: Editar
- Long press: Menu de contexto

**Chat Message**
- Enviar: Slide-up do input, aparece na lista
- Resposta IA: Typing indicator → fade-in
- Tap em link: Abre no navegador

### Estados

**Empty States**
- Lembretes: "Nenhum lembrete ainda. Diga 'Cria um lembrete' para começar!"
- Favoritos: "Seus atalhos aparecerão aqui"
- Chat: "Olá! Sou sua secretaryAI. Como posso ajudar?"

**Loading States**
- Skeleton com pulse violet
- Spinner circular com gradiente
- Progress bar para uploads

**Error States**
- Bluetooth desconectado: Banner amarelo com ícone
- Falha de conexão: Toast vermelho com retry
- Offline: Indicador no header + caches locais

## 5. Component Inventory

### Header
- **Default**: Logo + saudação + status BT
- **Scrolled**: Fica compacto com blur background
- **Error**: Ícone de alerta pulsante

### ActionCard
- **Default**: Ícone + título + descrição curta
- **Hover/Press**: Scale 0.98, sombra aumenta
- **Active**: Borda violet brilhante
- **Disabled**: Opacity 0.5, sem interação

### VoiceInput
- **Idle**: Microfone cinza com círculo suave
- **Listening**: Pulsos sonar cyan, ícone violet
- **Processing**: Spinner interno
- **Error**: Círculo vermelho com retry

### ChatBubble
- **User**: Alinhado à direita, fundo violet gradient
- **AI**: Alinhado à esquerda, fundo glass
- **System**: Centralizado, texto secondary

### ReminderCard
- **Default**: Título + hora + badge categoria
- **Done**: Check verde, texto riscado
- **Urgent**: Borda amber pulsante
- **Expired**: Opacity reduzida

### BottomNav
- **Item default**: Ícone outline + label secondary
- **Item active**: Ícone filled + cor violet + indicator dot
- **Item press**: Scale 0.9

### QuickShortcut
- **Default**: Círculo com ícone + label
- **Long press**: Edit mode com X para remover
- **Drag**: Lift effect com sombra

### BluetoothStatus
- **Connected**: Ponto verde + nome do dispositivo
- **Searching**: Spinner + "Buscando..."
- **Disconnected**: Ponto cinza + "Conectar"

## 6. Technical Approach

### Stack
- **Framework**: Next.js 14 (App Router)
- **Styling**: Tailwind CSS
- **Components**: Radix UI (primitives)
- **State**: React hooks + Context
- **Icons**: Lucide React
- **Animations**: Framer Motion

### Arquitetura

```
src/
├── app/
│   ├── layout.tsx          # Layout principal mobile
│   ├── page.tsx            # Home/Dashboard
│   ├── chat/page.tsx       # Tela de chat
│   ├── reminders/page.tsx  # Lembretes
│   ├── favorites/page.tsx  # Atalhos
│   └── settings/page.tsx   # Configurações
├── components/
│   ├── ui/                 # Componentes base
│   ├── features/           # Features específicas
│   └── layout/             # Layout components
├── hooks/
│   ├── useBluetooth.ts     # Hook de Bluetooth
│   ├── useVoiceInput.ts    # Hook de voz
│   └── useSecretary.ts     # Lógica da secretary
├── lib/
│   └── utils.ts
└── context/
    └── SecretaryContext.tsx
```

### Integração Bluetooth
- Web Bluetooth API para conexão
- Fallback para dispositivos simulados (demo mode)
- GATT services para Audio

### Integração Claude API
- Endpoint `/api/claude` com chave secreta
- Streaming de respostas
- Contexto conversacional mantido

### Data Model

**Reminder**
```typescript
{
  id: string
  title: string
  description?: string
  datetime: Date
  category: 'work' | 'personal' | 'health' | 'other'
  done: boolean
  createdAt: Date
}
```

**Shortcut**
```typescript
{
  id: string
  name: string
  trigger: string
  action: string
  icon: string
  category: string
}
```

**Message**
```typescript
{
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: Date
}
```

**Idea**
```typescript
{
  id: string
  title: string
  description: string
  notes: string
  createdAt: Date
  tags: string[]
}
```

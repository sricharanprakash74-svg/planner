# Planner Backend Service

Backend services for PlannerApp, providing personalized feed recommendations, social planning interactions, Stripe creator payouts, and an upstream AI proxy gateway.

## Tech Stack
- **Runtime**: Node.js (ES Modules)
- **Framework**: Express 5
- **ORM & Database**: Prisma ORM with SQLite (`prisma/schema.prisma`)
- **Payments**: Stripe API
- **AI Gateway**: Rate-limited proxy for Google Gemini & OpenAI

## Getting Started

### 1. Install Dependencies
```bash
npm install
```

### 2. Configure Environment Variables
Create a `.env` file based on `.env.example`:
```env
PORT=3000
STRIPE_SECRET_KEY=sk_test_mock
UPSTREAM_BASE_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent
UPSTREAM_API_KEY=your_api_key_here
CLIENT_SHARED_SECRET=shipaton_hackathon_token
```

### 3. Initialize Database
```bash
npx prisma generate
npx prisma db push
```

### 4. Run Development Server
```bash
npm run dev
```

### 5. Run Tests
```bash
npm test
```

## API Endpoints Overview
- `GET /api/feed`: Personalized algorithmic feed with anti-fatigue diversity guardrails.
- `POST /api/feed/interactions`: Log user interaction metrics (views, duration, likes, comments, adoptions).
- `GET /api/social/search`: Search public plans and creator profiles.
- `GET /api/social/plans`: Browse public plans (trending, most downloaded, recent, following).
- `POST /api/creator/sell-plan`: Register plan purchase and allocate creator earnings (70/30 split).
- `POST /api/creator/payout`: Request creator cash-out.
- `POST /api/proxy/generate`: Authenticated upstream AI proxy for plan generation.

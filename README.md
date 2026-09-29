# taxent

> An offline-first, social habit-building and routine-planning platform built for Android.

taxent bridges the gap between personal productivity and social accountability. It empowers users to design their structured multi-day plans, track micro-habits, visualize consistency through stylized performance charts, and discover community-crafted routines.

---

## 1. Core Problem taxent Solves

Most productivity and habit-tracking applications suffer from two fundamental problems:
1. **The Isolation Trap**: Habit formation in isolation leads to drop-offs. Without community reinforcement or creator-led frameworks, over 80% of personal development goals are abandoned within 30 days.
2. **Rigid, Online-Only Constraints**: Most modern planner apps fail in low-connectivity environments, introduce noticeable UI latency on cloud sync, or force users into subscription paywalls before demonstrating core utility.

taxent solves this by combining:
- **Instant Offline-First Architecture**: Fast Room SQLite persistence guarantees zero UI lag and complete offline functionality. Background synchronization with Supabase syncs progress seamlessly whenever connectivity is available.
- **Social Accountability & Creator Ecosystem**: Users can publish verified routines, fork curated plans from top creators, and earn rewards through streak freezes and community milestones.
- **Visual Performance Tracking**: Consistency curves translate everyday habit discipline into tangible momentum.
- **Sustainable Monetization via RevenueCat**: A frictionless, non-intrusive Pro tier powered by the RevenueCat SDK, featuring flexible annual monthly memberships and buying credits, streak protection shields, and cross-device entitlement restoration.

---

## 2. Key Features & User Flow

### Core User Flow
1. **Onboarding & Personalization**: Seamless Google One-Tap or email authentication, with an offline-first guest session fallback.
2. **Plan & Task Management**: Multi-tier folder and subtask hierarchy, daily check-ins, Pomodoro focus timer, and dynamic progress recalculations.
3. **Consistency & Analytics**: Financial-grade consistency stock charts, streak counters, completion velocity dashboards, and activity audit logs.
4. **Community & Explore**: Browse curated habit blueprints, fork public routines directly into your personal dashboard, and follow verified creators.
5. **Planner Pro Pass**: Integrated RevenueCat subscription paywall offering Pro entitlements, unlimited cloud sync, streak freeze shields, and multi-device access.

---

## 3. System Architecture & Technical Stack

taxent is built following modern Android Architecture Components and clean modular boundaries:

```
c:/projects/PlannerApp/
├── app/src/main/java/com/example/plannerapp/
│   ├── billing/          # RevenueCat integration & AppBillingRepository
│   ├── credits/          # Virtual credit economy, streak freezes, store
│   ├── database/         # Room SQLite entities, DAOs, converters
│   ├── network/          # Supabase client, Ktor HTTP engine, sync workers
│   ├── sync/             # WorkManager offline-to-cloud bidirectional sync
│   ├── theme/            # Material 3 design system, typography, colors
│   └── ui/               # Modular Compose feature screens & wrappers
│       ├── analytics/    # ConsistencyStockChartCard & dashboards
│       ├── auth/         # Google One-Tap & Supabase GoTrue Auth
│       ├── billing/      # PaywallScreen & RevenueCat subscription UI
│       ├── create/       # Plan and task creation workflows
│       ├── home/         # Unified feed, sliding tabs, folder cards
│       ├── navigation/   # Jetpack Navigation3 backstack & scaffold
│       ├── profile/      # Profile header, analytics & streak metrics
│       └── settings/     # Account management, export, & preferences
```

### Technology Highlights
- **Language**: Kotlin 2.1.0 / Java 17
- **UI Framework**: Jetpack Compose with Material Design 3
- **Navigation**: Android Jetpack Navigation 3 (Type-safe NavKey backstack)
- **Local Persistence**: Room SQLite 2.7.0 (KSP code-generation)
- **Cloud Backend**: Supabase (PostgreSQL, GoTrue Auth, Storage, Realtime)
- **In-App Purchases**: RevenueCat SDK (`com.revenuecat.purchases:purchases:10.20.0`)
- **Background Processing**: AndroidX WorkManager for background cloud sync
- **Image Loading**: Coil Compose 2.6.0
- **Design System**: Zero-emoji professional UI typography and vector iconography

---

## 4. Monetization & RevenueCat SDK Implementation

taxent implements monetization through the **RevenueCat SDK**:

- **Repository Layer** (`com.example.plannerapp.billing.AppBillingRepository`):
  - Configures `Purchases.configure()` asynchronously during app launch on IO Dispatchers.
  - Listens for realtime customer entitlement changes via `UpdatedCustomerInfoListener`.
  - Queries active offerings and packages via `Purchases.sharedInstance.getOfferings()`.
  - Executes purchases using `Purchases.sharedInstance.purchase()` with `PurchaseParams`.
  - Handles cross-device restore requests via `Purchases.sharedInstance.restorePurchases()`.
- **Entitlement Checks**:
  - `BillingConfig.ENTITLEMENT_PRO` (`"pro_access"`) and `BillingConfig.ENTITLEMENT_PREMIUM` (`"premium"`).
- **Graceful Sandbox & Mock Fallback**:
  - Next Gen / local build support: If running in development or on an emulator without a paid Google Play merchant account, mock billing seamlessly exercises the complete subscription lifecycle, entitlement persistence, and restoration flow.

---

## 5. Getting Started & Build Instructions

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK 36 (minSdk 23)

### Configuration
1. Clone the repository:
   ```bash
   git clone https://github.com/sricharanprakash74-svg/planner.git
   cd planner
   ```
2. Set up local credentials:
   Copy `local.properties.example` to `local.properties`:
   ```bash
   cp local.properties.example local.properties
   ```
3. Set your keys in `local.properties`:
   To keep credentials out of version control and follow Android best practices, taxent reads secrets dynamically from `local.properties` via `BuildConfig`.
   To run the app locally, add `REVENUECAT_KEY=your_key_here` to your `local.properties` file:
   ```properties
   # Supabase Credentials
   SUPABASE_URL=https://your-project.supabase.co
   SUPABASE_ANON_KEY=your-supabase-anon-key

   # RevenueCat Configuration
   REVENUECAT_KEY=test_your_actual_key_here
   USE_MOCK_BILLING=true
   ```

### Compile & Verify
Run Kotlin compilation directly via the Gradle wrapper:
```powershell
.\gradlew.bat compileDebugKotlin
```

---

## 6. License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

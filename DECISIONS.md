# FairShare Android — Architecture Decision Record

Every significant architectural decision made during development is recorded here.
Format: context, decision, reasoning, alternatives considered, consequences.

Last updated: March 25, 2026
Maintained by: Prathik Gouda

---

## Project Context

**App:** FairShare — Splitwise alternative, global audience
**Platform:** Android, Jetpack Compose
**Architecture:** MVVM + Clean Architecture (Presentation → Domain → Data)
**Backend:** Spring Boot 3.5, Java 25, PostgreSQL, JWT auth
**Backend package:** com.fairshare
**Backend base URLs:**
  - Development (emulator): `http://10.0.2.2:8080/`
  - Production: `https://api.fairshare.app/`
**JWT token lifetimes:**
  - Access token: 15 minutes
  - Refresh token: 7 days
**Total endpoints:** 93 across 13 controllers
**Target:** 36-day development plan, beta release May 13, 2026
**Team size:** Solo developer

---

## ADR-001 — Dark-only app, no light mode

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
The design system uses a dark surface palette (Surface0–Surface4) with green
(#22C97A) and orange (#FF6B35) accents. These colors only work on dark backgrounds.

**Decision:**
FairShare is dark-only. No light theme, no system theme toggle.

**Reasoning:**
Supporting both themes would double the design and QA surface area for a v1 beta.
The brand identity is tied to the dark aesthetic — it's a deliberate product decision,
not a technical limitation.

**Alternatives considered:**
- System-aware theme using `isSystemInDarkTheme()` — rejected, doubles QA surface
- Light theme only — rejected, doesn't match brand direction

**Consequences:**
- Users who prefer light mode will not be accommodated in v1
- All Compose composables use `MaterialTheme.colorScheme` which is always dark
- No conditional color logic needed anywhere in the UI layer

**Revisit:** After beta, if user feedback strongly requests it.

---

## ADR-002 — Syne + Inter typography

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
Typography is a core part of FairShare's visual identity. The app displays
financial data — amounts, names, dates — that needs to be legible at small sizes.

**Decision:**
Syne ExtraBold for all headings and large amounts. Inter for all body text,
labels, and form inputs.

**Reasoning:**
Syne gives FairShare a distinctive, modern identity that differentiates it from
Splitwise's generic sans-serif. Inter is widely considered the most legible body
font for dense UI text on screens — it was specifically designed for screen
readability at small sizes.

**Alternatives considered:**
- System fonts (Roboto) — rejected, too generic, no brand identity
- DM Sans — rejected, too similar to Roboto at small sizes
- Nunito — rejected, too rounded and casual for a financial app

**Consequences:**
- 9 .ttf files bundled in res/font/ (~800KB added to APK)
- Custom FontFamily objects in Type.kt for both fonts
- All headings and large amounts MUST use SyneFontFamily
- All body text MUST use InterFontFamily

---

## ADR-003 — EncryptedSharedPreferences for token storage

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
The app stores JWT access tokens (15min) and refresh tokens (7 days) locally.
These tokens grant full account access and must be protected at rest.

**Decision:**
Use `EncryptedSharedPreferences` (AES256-GCM) from `androidx.security:security-crypto`
instead of plain DataStore for token storage.

**Reasoning:**
Plain DataStore writes unencrypted protobuf files to the device's internal storage.
On a rooted device or with physical device access, these files are readable in
plaintext. A refresh token granting 7 days of access is effectively a password —
it must be encrypted at rest.

`EncryptedSharedPreferences` encrypts both key names and values using AES256-GCM
before writing to disk, backed by the Android Keystore system.

**Implementation:** `data/local/EncryptedTokenStore.kt`

**Alternatives considered:**
- Plain DataStore — rejected, tokens stored in plaintext on disk
- Android Keystore directly — too complex, EncryptedSharedPreferences uses
  Keystore internally anyway
- Room with encryption — overkill for 3 simple key-value pairs

**Consequences:**
- Tokens are synchronous reads/writes (no suspend functions needed)
- AuthInterceptor reads tokens without runBlocking complications
- Slightly slower first access due to key derivation (~50ms, negligible)
- Token store is a @Singleton injected via DataStoreModule

---

## ADR-004 — ApiResult sealed class instead of kotlin.Result

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
Repository and use case functions need to communicate both success and various
failure modes to ViewModels, which then drive UI state.

**Decision:**
Use a custom `ApiResult<T>` sealed class for all repository and use case return
types instead of `kotlin.Result<T>`.

**Reasoning:**
`kotlin.Result` has two states: success and failure. A financial app needs to
distinguish between:
- Network failure → "Check your connection" + retry button
- 401 → clear tokens, navigate to Login silently
- 400 validation → show field-level errors inline on form
- 409 conflict → show business rule message ("email already registered")
- 404 → show empty state
- 500 → show generic error

With `kotlin.Result`, all failures look the same. The ViewModel has to parse
exception messages — fragile and error-prone.

**Implementation:** `domain/model/ApiResult.kt`

**States:**
```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T)
    data class ValidationError(val message: String, val errors: List<FieldError>)
    data class Unauthorized(val message: String)
    data class Forbidden(val message: String)
    data class NotFound(val message: String)
    data class Conflict(val message: String)
    data class HttpError(val code: Int, val message: String)
    data class NetworkError(val exception: Throwable, val message: String)
}
```

**Alternatives considered:**
- `kotlin.Result<T>` — rejected, insufficient error granularity
- Sealed `Either<Error, T>` — rejected, reinvents what ApiResult already does
- Exception-based error handling — rejected, not idiomatic in coroutines

**Consequences:**
- All 11 repository interfaces return `ApiResult<T>`
- All 34 use cases return `ApiResult<T>`
- ViewModels use `when (result)` with compiler-enforced exhaustiveness
- `safeApiCall()` in the data layer maps HTTP error codes to ApiResult types

---

## ADR-005 — Keep all 34 use cases

**Date:** March 25, 2026
**Status:** Accepted (overriding external feedback)

**Context:**
External code review suggested trimming from 34 use cases to 7, deleting
"pass-through" use cases that simply delegate to repositories.

**Decision:**
Keep all 34 use cases.

**Reasoning:**
Use cases are the correct architectural layer for:
- Input validation (already implemented on Login, Register, CreateExpense)
- Analytics event firing when business actions occur
- Caching decisions (should this read from Room or network?)
- Rate limiting (don't refetch if last fetch was < 30 seconds ago)
- Feature flags (is this feature enabled for this user?)

Deleting use cases now because they're "simple" means re-creating them when
any of the above logic needs to be added. The cost of keeping them is 34 files.
The cost of retrofitting them later is refactoring every ViewModel that calls
the repository directly.

**Alternatives considered:**
- Trim to 7 use cases (external feedback recommendation) — rejected
- ViewModels call repositories directly for reads — rejected, breaks layering

**Consequences:**
- 34 use case files, most are thin wrappers today
- Input validation is consistently in use cases, not ViewModels
- Adding business logic later requires changing one file, not many

---

## ADR-006 — Double for monetary amounts, not Long cents

**Date:** March 25, 2026
**Status:** Accepted (with known risk)

**Context:**
The backend uses `BigDecimal` for amounts, serialized as JSON numbers (doubles).
Some architectures recommend converting to Long cents on the client to avoid
floating point issues.

**Decision:**
Use `Double` for all monetary amounts in DTOs and domain models.
Use `MoneyUtils.kt` for all display formatting — never format money manually.

**Reasoning:**
The backend handles all split calculations and returns per-person amounts.
The Android app never performs monetary arithmetic — it only displays amounts
received from the backend. This eliminates the primary risk of floating point errors.

Converting to Long cents adds complexity at every mapper, every input parser,
and every display call for marginal benefit in v1.

**Known risk:**
Three-way split rounding ($10 ÷ 3 = $3.33 + $3.33 + $3.34) is handled by the
backend, not the client. If the backend has rounding bugs, the client reflects them.

**Alternatives considered:**
- Long cents everywhere — rejected, too much complexity for v1
- BigDecimal on Android — rejected, no JSON serialization support in kotlinx

**Consequences:**
- `MoneyUtils.format()` MUST be used for all monetary display
- Never use string interpolation like `"$$amount"` anywhere in the codebase
- Revisit before v1.0 production release

---

## ADR-007 — Single source of truth for enums in domain/model

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
Initial implementation had enums duplicated in both `domain/model/` (inside
model files) and `data/model/enums/` (for serialization). This caused compile
errors and confusion about which enum to import.

**Decision:**
All enums live exclusively in `domain/model/` as separate files.
`data/model/enums/` has been deleted entirely.
Data layer DTOs import enums from `domain/model/`.
Domain layer never imports from data layer.

**The 11 enums in domain/model:**
SplitType, ExpenseCategory, GroupType, SettlementStatus, FriendStatus,
FriendshipType, AuthProvider, AccountStatus, NotificationType,
ReminderFrequency, SettleType

**Reasoning:**
These are all business concepts, not network concerns. Having them in
`data/model/enums/` would require domain to import from data — a dependency
direction violation in Clean Architecture. The domain layer must be
independent of the data layer.

**Rule:**
- Business concept enum → `domain/model/`
- Purely network/serialization concern → `data/model/` (none currently exist)

**Consequences:**
- All imports reference `com.prathik.fairshare.domain.model.SplitType` etc.
- Single place to add/remove enum values
- `data/model/enums/` must never be recreated

---

## ADR-008 — @Parcelize on all domain models

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
Jetpack Compose Navigation requires objects passed as navigation arguments to
be serializable across process death and restoration.

**Decision:**
All domain models that cross navigation boundaries implement `Parcelable`
via the `@Parcelize` Kotlin compiler plugin annotation.

**Plugin:** `id("kotlin-parcelize")` in app/build.gradle.kts

**Models with @Parcelize:**
User, Group, Expense, Balance, Settlement, Friend, Friendship, Notification,
GroupMember, Receipt, Reminder, ExpenseItem, ExpenseComment + all nested classes

**Reasoning:**
`@Parcelize` generates all `writeToParcel()`, `createFromParcel()`, and
`CREATOR` boilerplate automatically. Manual implementation is error-prone
and creates maintenance burden when fields are added.

**Alternatives considered:**
- Pass only IDs between screens, re-fetch on destination — valid but causes
  extra network calls and loading states on every navigation
- `@Serializable` for navigation — requires additional setup, @Parcelize
  is the Android-native solution

**Consequences:**
- kotlin-parcelize plugin must remain in build.gradle.kts
- All new domain models that cross nav boundaries must add @Parcelize
- Nested classes inside domain models also need @Parcelize

---

## ADR-009 — Coarse location only, asked contextually

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
FairShare is a global app. Currency auto-detection improves UX significantly —
users traveling abroad shouldn't manually change currency when adding expenses.

**Decision:**
Request `ACCESS_COARSE_LOCATION` only when the user creates a TRIP group.
Never request `ACCESS_FINE_LOCATION`. Fall back to device locale on first launch.

**Permission in AndroidManifest.xml:**
`android.permission.ACCESS_COARSE_LOCATION`

**Fallback chain:**
1. Physical location (if permission granted) → country code → currency
2. Device locale → currency via `Currency.getInstance(Locale.getDefault())`
3. "USD" hardcoded default

**Reasoning:**
Location permission is the most sensitive permission on Android. Users deny it
when asked upfront before any action. Asking in the context of creating a TRIP
group ("to auto-detect local currency while traveling") is contextually justified
and has higher acceptance rates.

Coarse location (cell tower / WiFi) is sufficient for country-level detection.
Fine location (GPS) would be invasive and provide no additional benefit.

**Implementation:** `util/LocationHelper.kt`

**Consequences:**
- Covers 60+ countries in `countryToCurrency` map
- Permission dialog shown only on TRIP group creation
- No location data is sent to the backend — detection is client-side only
- Users can always manually override currency in EditProfile

---

## ADR-010 — WebSocket + offline queue are core features, not post-beta

**Date:** March 25, 2026
**Status:** Accepted (overriding initial plan)

**Context:**
Initial plan classified WebSocket real-time sync and offline action queue as
post-beta features to reduce scope.

**Decision:**
Both ship in v1 beta as Phase 7 (Apr 29 – May 5).

**Reasoning:**
FairShare is a shared finance app used by groups. If one person adds an expense
and others don't see it until they manually pull to refresh, the app feels broken.
If the app silently fails when someone tries to settle up on the subway — it's
untrustworthy for financial data.

These aren't nice-to-haves. They are what separates a demo from a production app.

**WebSocket design:**
- Backend: Spring Boot + STOMP over WebSocket
- Topics: `/topic/group/{groupId}`, `/topic/balance/{userId}`
- Events: ExpenseCreated, ExpenseUpdated, ExpenseDeleted, SettlementRecorded,
  MemberJoined, BalanceUpdated
- Auto-reconnect with exponential backoff (1s, 2s, 4s, 8s, max 30s)

**Offline queue design:**
- Room-backed `PendingAction` entity
- Actions: CREATE_EXPENSE, SETTLE, etc.
- Drain on connectivity restored (FIFO, max 3 retries per action)
- Optimistic updates — write locally immediately, queue network call

**Consequences:**
- Backend WebSocket endpoints must be built before Day 26
- Additional dependencies: STOMP client library
- Room database required from Day 8 onwards
- Phase 7 is non-negotiable scope

---

## ADR-011 — Mapper pattern in data/model/mapper/

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
Repository implementations need to convert response DTOs to domain models.
Without a defined pattern, this conversion logic gets duplicated across every
repository method.

**Decision:**
All DTO → domain model conversions happen in dedicated extension functions
in `data/model/mapper/`. Repository implementations call `.toDomain()`.

**The 11 mapper files:**
UserMapper, GroupMapper, ExpenseMapper, BalanceMapper, SettlementMapper,
FriendMapper, NotificationMapper, ReceiptMapper, ReminderMapper,
ExpenseItemMapper, ExpenseCommentMapper

**Pattern:**
```kotlin
// In GroupMapper.kt (data layer)
fun GroupResponse.toDomain(): Group = Group(
    id = id,
    name = name,
    // ...
)

// In GroupRepositoryImpl.kt
return ApiResult.Success(response.data.toDomain())
```

**Reasoning:**
Without mappers, `GroupRepositoryImpl.getMyGroups()`, `getGroup()`,
`createGroup()`, and `updateGroup()` all contain identical conversion code.
A single field rename requires updating 4+ places. Mappers give one place
for conversion, null safety, and enum mapping.

**Consequences:**
- `data/model/mapper/` must be updated when domain models change
- Repository implementations are clean — just API call + `.toDomain()`
- New domain models need a corresponding mapper file

---

## ADR-012 — Locale-aware formatting from day one

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
FairShare targets global users. Currency formatting, date display, and number
formatting differ significantly across locales.

**Decision:**
- All monetary amounts: `MoneyUtils.format(amount, currencyCode)` — uses
  `NumberFormat.getCurrencyInstance(locale)` — never manual string formatting
- All timestamps: `DateFormatter` with `ZoneId.systemDefault()` — never
  display raw UTC strings
- All strings: `strings.xml` — no hardcoded English strings in composables
- All padding/alignment: `start/end` not `left/right` — RTL support

**Examples of why this matters:**
- India: ₹1,23,456.78 (different digit grouping)
- Germany: 1.234,56 € (symbol after, comma decimal)
- Japan: ¥1,235 (no decimal places)
- Arabic: right-to-left layout

**Implementation:**
- `util/MoneyUtils.kt` — locale-aware currency formatting
- `util/DateFormatter.kt` — timezone-aware timestamp formatting
- `util/LocationHelper.kt` — country detection for currency defaults

**Consequences:**
- `MoneyUtils.format()` MUST be used everywhere — never `"$$amount"`
- `DateFormatter` MUST be used for all timestamp display
- RTL must be tested before beta release
- strings.xml maintained from day one

---

## ADR-013 — Biometric auth for large settlements

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
Settle Up is a financial action that moves money between users. Accidental
taps on the settle button could record unintended payments.

**Decision:**
Require biometric authentication (fingerprint / face) before processing
settlements above a configurable threshold (default $50).

**Implementation:** `BiometricHelper.kt` in the auth package, used by
`SettleUpScreen.kt` and `PartialSettleScreen.kt`.

**Reasoning:**
$50 is a meaningful amount that warrants confirmation. Below $50 the friction
of biometric auth outweighs the benefit. The threshold will be made
user-configurable in a future version.

**Fallback:** If the device has no biometric hardware, skip the biometric
check and proceed directly.

**Consequences:**
- `androidx.biometric:biometric` dependency required
- BiometricHelper injected into SettleViewModel
- Biometric prompt shown before API call, not after

---

## ADR-014 — SettleType enum replaces raw String

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
`SettleRequest.type` was originally a raw `String` accepting "ALL", "GROUP",
"NON_GROUP", or "PARTIAL". Raw strings bypass compile-time safety.

**Decision:**
Created `SettleType` enum in `domain/model/` with values ALL, GROUP, NON_GROUP,
PARTIAL. `SettleRequest.type` is now `SettleType`.

**Reasoning:**
A typo like `"GRUP"` instead of `"GROUP"` compiles and fails silently at runtime.
An enum makes invalid values a compile error.

**Note on serialization:**
Kotlinx Serialization serializes enums to their name string by default —
`SettleType.GROUP` → `"GROUP"` — which matches what the backend expects.

**Consequences:**
- `SettleType` in `domain/model/SettleType.kt`
- `SettleRequest` updated to use `SettleType` not `String`
- `SettleUseCase` validates GROUP requires groupId, PARTIAL requires amount + currency

---

## ADR-015 — safeApiCall() pattern for network error mapping

**Date:** March 25, 2026
**Status:** Accepted (to be implemented Day 7)

**Context:**
Every repository implementation needs to wrap network calls in try-catch,
parse error response bodies, and map HTTP status codes to ApiResult types.
Duplicating this logic in 10+ repository implementations is error-prone.

**Decision:**
Create a `safeApiCall()` extension function in the data layer that:
1. Wraps the call in try-catch
2. On IOException → returns `ApiResult.NetworkError`
3. On HttpException → parses the response body as `ApiResponse<Unit>`
4. Maps status codes: 400→ValidationError, 401→Unauthorized, 403→Forbidden,
   404→NotFound, 409→Conflict, other→HttpError

**Pattern:**
```kotlin
// Every repository method
suspend fun login(email: String, password: String): ApiResult<User> =
    safeApiCall { authService.login(LoginRequest(email, password)) }
        .map { it.data!!.toDomain() }
```

**Consequences:**
- Network error handling is in one place
- Repository implementations are 3-5 lines per method
- Adding new error codes requires changing safeApiCall() only

---

## ADR-016 — Certificate pinning via OkHttp

**Date:** March 25, 2026
**Status:** Accepted (to be implemented Day 8)

**Context:**
FairShare transmits financial data. Man-in-the-middle attacks intercepting
API traffic would expose expense amounts, user data, and JWT tokens.

**Decision:**
Configure `OkHttpClient.CertificatePinner` with SHA-256 pins for
`api.fairshare.app` in `NetworkModule.kt`.

**Reasoning:**
Certificate pinning prevents MITM attacks even if a compromised CA is trusted
by the device. Standard practice for apps handling financial data.

**Note:** Certificate pins must be updated before certificate expiry.
Calendar reminder set for certificate renewal.

**Consequences:**
- Pins hardcoded in NetworkModule — must be updated on certificate renewal
- Debug builds bypass pinning via BuildConfig check
- Certificate rotation requires app update

---

## ADR-017 — GitHub Actions CI/CD pipeline

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
Solo development with no automated checks means style drift and broken builds
go undetected until manually noticed.

**Decision:**
GitHub Actions pipeline runs on every PR and push to main:
1. ktlint — code style enforcement
2. ./gradlew build — full build verification
3. ./gradlew test — unit test suite

**Reasoning:**
Even as a solo developer, CI/CD catches:
- Forgetting to run tests before pushing
- Style inconsistencies between sessions
- Build-breaking changes caught before they accumulate

**Consequences:**
- PRs cannot merge with failing CI
- ktlint rules enforced consistently
- ~3-5 minute CI feedback loop per push

---

## ADR-018 — 93 endpoints across 13 Retrofit service interfaces

**Date:** March 25, 2026
**Status:** Accepted

**Context:**
The backend has 93 endpoints across 14 controllers. Each needs a
corresponding Retrofit interface method.

**Decision:**
Create 13 Retrofit service interfaces (excluding HealthController):
AuthApiService, UserApiService, GroupApiService, ExpenseApiService,
BalanceApiService, SettlementApiService, FriendApiService,
NotificationApiService, AnalyticsApiService, ReceiptApiService,
ReminderApiService, ImportApiService, ExpenseCommentApiService

One interface per backend controller. All registered as singletons in
`NetworkModule.kt`.

**Consequences:**
- 13 @Singleton service instances in the Hilt graph
- Each repository implementation injects only its relevant service(s)
- Adding endpoints requires updating the corresponding service interface only

---

## ADR-019 — Known backend quirks

**Date:** March 25, 2026
**Status:** Reference

**These backend behaviors differ from what you might expect:**

1. **CreateGroupRequest uses "description" not "notes"** — verified from
   backend source. Do not use "groupNotes" or "notes" as the field name.

2. **Settlements are immediately COMPLETED** — no PENDING → confirm step.
   `completedAt` will equal `settlementDate` in most cases.

3. **Balance sign convention** — positive = other user owes you (green),
   negative = you owe other user (orange). This is consistent everywhere.

4. **ExpenseResponse.payers and splits are null on list endpoints** — the
   backend omits nested arrays for performance on list calls. They are only
   populated on single expense fetch (`GET /api/expenses/{id}`).
   Mappers default these to `emptyList()`.

5. **Backend timestamp format** — `"2026-03-25T14:30:00"` without Z suffix
   on some endpoints. `DateFormatter.parseTimestamp()` handles both formats.

6. **SettleRequest.type is a String on the backend** — but we use `SettleType`
   enum on Android. Kotlinx Serialization serializes the enum name to string
   automatically, which matches what the backend expects.

---

*This document is a living record. Add an entry for every significant decision
made during development. Small decisions don't need entries — only ones that
future developers would question or that have non-obvious reasoning.*

[DECISION] Google/Apple OAuth deferred to Day 26 (Polish phase).
Requires Google Cloud Console OAuth client ID setup outside of coding scope.
Backend /api/auth/oauth endpoint is ready and waiting.
Android: add google-auth dependency + configure client ID + wire OAuthLoginRequest.
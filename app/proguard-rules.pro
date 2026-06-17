# ==============================================================
# FairShare — ProGuard / R8 rules
# ==============================================================
#
# Most libraries ship their own consumer rules inside their AARs,
# so this file stays deliberately minimal. Rules are only added
# here when a specific, confirmed release-only breakage risk
# exists that is NOT covered by library-bundled rules.
#
# Libraries whose consumer rules already handle everything:
#   - Hilt / Dagger (@HiltAndroidApp, @AndroidEntryPoint, @HiltViewModel)
#   - Room (@Entity, @Dao, @Database)
#   - Retrofit 2 (interface proxies, annotations)
#   - kotlinx.serialization (KSP-generated serializers, @SerialName)
#   - Coil (custom decoders/fetchers)
#   - Kotlin Parcelize (@Parcelize, CREATOR)
# ==============================================================


# ── 1. Stack trace readability ─────────────────────────────────
#
# Without these two attributes, crash reports from Play Console
# and Firebase Crashlytics show only obfuscated method names and
# no line numbers — impossible to debug in production.
#
# SourceFile is renamed to "SourceFile" (not the real filename) by
# the line below, which prevents reverse-engineering the class layout
# while still making stack traces actionable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile


# ── 2. WorkManager workers (SyncWorker, CacheWarmupWorker) ─────
#
# WorkManager instantiates workers by their fully-qualified class name,
# which is stored as a plain string in the WorkRequest. If R8 renames
# or removes these classes, WorkManager throws a ClassNotFoundException
# at runtime and the sync queue silently stops working — no crash,
# just broken offline sync and broken cache warmup on login.
#
# @HiltWorker workers use @AssistedInject, which also requires the
# constructor to survive obfuscation so the HiltWorkerFactory can
# create instances. The hilt-work library ships consumer rules for
# the factory wiring, but not for the concrete worker class names.
-keep class com.prathik.fairshare.data.sync.SyncWorker { *; }
-keep class com.prathik.fairshare.data.sync.CacheWarmupWorker { *; }


# ── 3. Strip Android Log calls from release builds ─────────────
#
# The codebase contains android.util.Log.d / Log.w calls in repository,
# cache, sync, expense, settlement, and balance paths. These log entries
# can expose sensitive financial state (balances, group IDs, user IDs,
# token lifetimes) in production logcat on any device with ADB access.
#
# -assumenosideeffects tells R8 that these methods have no observable
# side effects — R8 then removes all call sites entirely in release builds.
#
# Debug builds are NOT affected: minifyEnabled is false in the debug
# build type, so this rule is never applied during development and
# all logs remain visible in Android Studio Logcat as normal.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}


# ── 4. App enum constant names ─────────────────────────────────
#
# Several mappers parse backend string values into app enums via
# Enum.valueOf(wireString), e.g. SplitType.valueOf("EQUAL"),
# GroupType.valueOf("GROUP"), SettlementStatus.valueOf("SETTLED").
# Each call is wrapped in try/catch with a fallback default, so if
# R8 ever renamed an enum constant the failure would be SILENT:
# every wire value would collapse into its fallback (e.g. "GROUP"
# → NON_GROUP, "FOOD" → OTHER) instead of crashing — a release-only
# correctness bug invisible in debug (where minify is off).
#
# Keep the constant fields of the app's own enums so valueOf(...)
# always resolves against the original names. This is intentionally
# narrow (com.prathik.fairshare enums only) — it does NOT keep DTOs,
# models, or any other classes.
-keepclassmembers enum com.prathik.fairshare.** {
    public static final ** *;
}
# RealPredict AI

> **Datos. Patrones. Evidencia.**
>
> Aplicación Android nativa para análisis estadístico de la **Quiniela Real de República Dominicana, sorteo de la 1:00 p.m.**
> Local-first, multimodelo (OpenAI / Gemini / Mistral / Claude) y con evaluación estadística verificable.

---

## Estado del proyecto

| Componente | Estado | Módulo |
|---|---|---|
| UI (Jetpack Compose + Material 3) | ✅ Implementado | `:app` |
| Common (Result, DispatcherProvider, TimeProvider, Constants) | ✅ Implementado | `:core-common` |
| Domain (entidades, repositorios, use cases) | ✅ Implementado | `:core-domain` |
| Statistics Engine (frecuencia, ausencias, parejas, outliers, backtest) | ✅ Implementado + tests | `:core-statistics` |
| Data (Room, DAOs, importadores CSV/XLSX, exportador CSV) | ✅ Implementado | `:core-data` |
| Security (CryptoManager con Android Keystore) | ✅ Implementado | `:core-security` |
| AI Gateway (4 adaptadores + validación de respuesta) | ✅ Implementado | `:core-ai` |
| GitHub Actions workflow (build + test) | ✅ Configurado | `.github/workflows/build.yml` |

La interfaz ya está completa en `:app` y NO se modificó. Los módulos del motor son aditivos.

---

## Estructura del repositorio

```
RealPredict-AI/
├── app/                      # UI (ya existente — sin tocar)
│   └── src/main/java/com/realpredict/ai/
│       ├── ui/screens/       # 9 pantallas (Home, Prediction, Statistics, etc.)
│       ├── ui/theme/         # Color, Motion, Theme
│       ├── ui/components/    # LotteryBall, RealPredictBottomBar
│       ├── data/local/       # DrawEntity, DrawDao, RealPredictDatabase (v1)
│       ├── data/security/    # KeystoreManager (legacy — ver :core-security)
│       └── domain/model/     # Draw, PredictionRecord (legacy — ver :core-domain)
│
├── core-common/              # NUEVO — utilidades compartidas (Kotlin/JVM puro)
│   └── Result, DispatcherProvider, TimeProvider, Constants
│
├── core-domain/              # NUEVO — entidades + contratos (Kotlin/JVM puro)
│   ├── model/                # Draw, ImportBatch, PredictionRecord, PredictionEvaluation,
│   │                         #   ProviderConfiguration, ChatConversation/Message, Experiment
│   ├── repository/           # DrawRepository, PredictionRepository, EvaluationRepository, BacktestRepository
│   └── usecase/              # contratos pendientes (ver roadmap)
│
├── core-statistics/          # NUEVO — motor estadístico determinista (Kotlin/JVM puro)
│   ├── FrequencyCalculator
│   ├── AbsenceCalculator
│   ├── PairAnalyzer
│   ├── OutlierDetector
│   ├── BacktestEngine        # antifuga: training anterior, predicción sellada, evaluación cronológica
│   ├── SelectionStrategy    # TopFrequencyStrategy + RandomStrategy (baseline)
│   └── StatisticsEngine      # fachada
│
├── core-data/                # NUEVO — persistencia Room + import/export (Android Library)
│   ├── local/entity/         # 7 entidades (ImportBatch, Prediction, Evaluation, ProviderConfig, Chat*, Experiment)
│   ├── local/dao/            # DAOs con operaciones inmutables (PredictionDao NO permite UPDATE)
│   ├── local/EngineDatabase  # RoomDatabase v1
│   ├── importer/              # CsvImporter + XlsxImporter (con auto-detección de columnas)
│   └── exporter/             # CsvExporter (draws, predictions, evaluations, backtest)
│
├── core-security/            # NUEVO — cifrado de API Keys (Android Library)
│   └── CryptoManager         # EncryptedSharedPreferences + MasterKey + redacción de logs
│
├── core-ai/                  # NUEVO — capa común de IA (Android Library)
│   ├── AiProvider            # interfaz común
│   ├── adapters/             # OpenAIAdapter, GeminiAdapter, MistralAdapter, ClaudeAdapter
│   └── validation/           # ResponseValidator + SystemPrompt obligatorio
│
├── gradle/
│   ├── libs.versions.toml    # version catalog centralizado
│   └── wrapper/              # gradle-wrapper.jar + properties (Gradle 8.7)
│
├── .github/workflows/build.yml  # CI: compila JVM + Android, corre tests, sube APK
├── settings.gradle.kts       # incluye :app + 6 módulos core-*
└── build.gradle.kts          # registra plugins compartidos
```

---

## Cómo compilar

### Requisitos

- JDK 17 (Temurin u OpenJDK)
- Android Studio Iguana+ (o Gradle 8.7 desde consola)
- Android SDK 35 (compileSdk/targetSdk) + build-tools 35.0.0

### Build local

```bash
# Setup inicial (solo la primera vez)
chmod +x gradlew

# Compilar APK de debug
./gradlew assembleDebug

# Correr tests unitarios (JVM puros — no requieren Android SDK)
./gradlew :core-statistics:test :core-domain:test :core-common:test

# Correr tests de Android (requiere Android SDK)
./gradlew testDebugUnitTest

# Lint
./gradlew lintDebug
```

El APK resultante estará en `app/build/outputs/apk/debug/app-debug.apk`.

### Conectar la UI con el motor

La UI actual en `:app` no depende aún de los módulos del motor. Para integrarlos:

1. Edita `app/build.gradle.kts` y descomenta las líneas de `project(":core-*")`.
2. Refactoriza los viewmodels/screens para usar los repositorios de `:core-domain`.
3. Migra `RealPredictDatabase` de `:app` a `EngineDatabase` de `:core-data` (la base de datos unificada).
4. Reemplaza `KeystoreManager` por `CryptoManager` de `:core-security` (interfaz compatible).
5. Para invocar IA desde la UI, usa `AiProvider` con cualquier adaptador de `:core-ai`.

---

## GitHub Actions

El workflow `.github/workflows/build.yml` corre automáticamente en `push` a `main`/`develop` y en PRs:

1. **jvm-build**: compila y testea módulos Kotlin/JVM puros (`:core-common`, `:core-domain`, `:core-statistics`) — sin Android SDK.
2. **android-build**: instala Android SDK, compila el APK de debug, corre tests de Android y lint. Sube el APK como artifact.
3. **summary**: pasa solo si todos los checks anteriores pasaron.

Artefactos generados en cada build:
- `app-debug-apk` (30 días)
- `jvm-test-results`, `android-test-results`, `lint-reports` (14 días)

---

## Reglas de integridad

Siguiendo la Especificación Técnica:

1. **Inmutabilidad de predicciones**: `PredictionDao` solo permite `@Insert`. No hay `@Update` ni `@Delete`. Las correcciones crean nueva versión (`previousVersionId`).
2. **Antifuga en backtesting**: `BacktestEngine.run()` entrena solo con sorteos anteriores al target. La predicción se sella ANTES de consultar el resultado.
3. **Validación ceros iniciales**: números se conservan como `String "00".."99"` en toda la cadena. Los importadores (CSV/XLSX) hacen `padToTwoDigits()`.
4. **Sin claves en texto plano**: `CryptoManager` usa `EncryptedSharedPreferences` con `MasterKey` AES256-GCM respaldado por Android Keystore.
5. **Validación de respuesta de IA**: `ResponseValidator.verifyAgainstStatistics()` rechaza respuestas que contradicen el motor estadístico.
6. **Determinismo**: `FrequencyCalculator`, `AbsenceCalculator`, `BacktestEngine` y `SelectionStrategy` producen el mismo resultado para la misma entrada.

---

## Limitaciones conocidas

- Los años **2020 a 2025** de la Quiniela Real La Una NO están disponibles en fuentes abiertas verificables. El dataset CC BY 4.0 de `loteriadela1.com` solo cubre desde `2026-07-07`.
- Los adaptadores de IA (OpenAI, Gemini, Mistral, Claude) están implementados pero no se han probado con credenciales reales — requieren que el usuario ingrese su propia API Key.
- Los `usecase/` en `:core-domain` están pendientes (solo se proveen contratos). La lógica se invoca directamente desde el motor estadístico en esta versión.
- No hay backend: toda la operación es local, como exige la Sección 2.

---

## Licencia

El código fuente está bajo licencia MIT (ver `LICENSE`). Los datos históricos de la Quiniela Real, cuando se importen, están sujetos a la licencia de su fuente original.

**No se incluyen API Keys ni datos personales en este repositorio.**

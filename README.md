# RealPredict AI — Quiniela Real (1:00 p. m.)

> **Datos. Patrones. Evidencia.**  
> Aplicación Android nativa, local-first, multimodelo y con evaluación estadística verificable para la Quiniela Real de República Dominicana (sorteo de la 1:00 p. m.).

---

## 📱 Estructura del Proyecto Android Nativo

Este repositorio contiene exclusivamente el proyecto nativo Android listo para compilarse en Android Studio o por línea de comandos:

```
.
├── app/
│   ├── build.gradle.kts          # Jetpack Compose, Material 3, Room, Lottie, Keystore
│   ├── proguard-rules.pro        # Reglas ProGuard para optimización de release
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── res/values/       # colors.xml, strings.xml, themes.xml
│           └── java/com/realpredict/ai/
│               ├── MainActivity.kt        # Navegación con Animated NavHost & BottomBar
│               ├── RealPredictApp.kt      # Application class (Room + Keystore)
│               ├── ui/
│               │   ├── components/        # LotteryBall 3D, RealProgressIndicator, Lottie
│               │   ├── screens/           # Las 9 pantallas de la especificación
│               │   │   ├── HomeScreen.kt
│               │   │   ├── PredictionScreen.kt
│               │   │   ├── StatisticsScreen.kt
│               │   │   ├── LaboratoryScreen.kt
│               │   │   ├── ChatScreen.kt
│               │   │   ├── ImportScreen.kt
│               │   │   ├── HistoryScreen.kt
│               │   │   ├── ApiKeysScreen.kt
│               │   │   └── SettingsScreen.kt
│               │   └── theme/             # Color.kt, Theme.kt, Motion.kt (Spec 19)
│               ├── domain/model/          # Draw, PredictionRecord, Experiment
│               └── data/
│                   ├── local/             # Room Database, DrawDao, DrawEntity
│                   └── security/          # KeystoreManager (Cifrado de claves)
├── build.gradle.kts              # Plugins de Gradle para Android y Compose
├── settings.gradle.kts           # Configuración de repositorios Google & MavenCentral
├── gradle.properties             # Opciones de memoria JVM y AndroidX
└── README.md
```

---

## 🛠️ Cómo Compilar el APK Nativo

### 1. Con Android Studio:
1. Abre **Android Studio** (versión Ladybug o superior).
2. Selecciona **Open** y abre la carpeta raíz de este repositorio.
3. Espera a que Gradle sincronice las dependencias.
4. Conecta tu teléfono Android o inicia un emulador.
5. Haz clic en **Run 'app'** o en el menú: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

### 2. Por Línea de Comandos (Gradle):

```bash
# Compilar APK de depuración
./gradlew assembleDebug

# Compilar APK optimizado para release
./gradlew assembleRelease
```

El APK se generará en:
`app/build/outputs/apk/release/app-release-unsigned.apk`

---

## ⚙️ Especificaciones Técnicas

- **Plataforma**: Android Nativo
- **Lenguaje**: Kotlin 2.0
- **UI Toolkit**: Jetpack Compose & Material 3
- **Base de Datos**: Room sobre SQLite (cadenas `"00"` a `"99"` con ceros iniciales)
- **Seguridad**: Android Keystore con `EncryptedSharedPreferences` (AES-256)
- **Animaciones**: Compose Animation + Lottie for Compose
- **Formatos de Importación**: CSV y XLSX mediante Android Storage Access Framework

# Baúl Sanitario

Aplicación móvil Android de uso personal para la digitalización, clasificación y almacenamiento seguro de documentos médicos familiares. Permite capturar recetas, exámenes, boletas y órdenes médicas directamente desde el celular usando la cámara como escáner, organizando cada documento bajo el perfil familiar correspondiente y almacenándolo de forma estructurada en la nube.

## Stack tecnológico

El proyecto se desarrolla íntegramente en Kotlin con interfaz declarativa construida sobre Jetpack Compose. La captura y procesamiento de documentos se realiza de forma local mediante la Google ML Kit Document Scanner API, que entrega el resultado como PDF con apariencia real de documento escaneado. La persistencia remota se apoya en Supabase, utilizando su módulo de base de datos relacional (PostgreSQL) para el registro de metadatos y su módulo de Storage para el almacenamiento binario de los archivos. La comunicación asíncrona y el manejo de concurrencia se implementan con Kotlin Coroutines y Flow. Para las llamadas de red al cliente de Supabase se usa Ktor. Room se contempla como capa de caché local opcional para soporte offline.

## Arquitectura prevista

La aplicación sigue los principios de Clean Architecture organizados en tres capas desacopladas dentro del módulo Android:

La capa de presentación contiene las pantallas Compose y los ViewModels. Es la única capa que conoce el framework de UI. Los ViewModels exponen StateFlow consumidos directamente por los composables y delegan toda la lógica al dominio.

La capa de dominio contiene los casos de uso y las interfaces de repositorio. No tiene dependencias de Android ni de frameworks externos. Aquí reside la regla de negocio central: la consistencia transaccional entre la subida del archivo al Storage y el registro posterior de sus metadatos en la base de datos relacional.

La capa de datos implementa los repositorios definidos en el dominio. Contiene el cliente de Supabase, los DTOs de red y la lógica de mapeo hacia los modelos del dominio. También incluye el DAO de Room si se activa la caché local.

El almacenamiento en Supabase Storage sigue una jerarquía lógica por perfil y tipo de documento: `bucket / perfil_id / tipo_documento / archivo.pdf`.

## Estructura de carpetas del código fuente

```
app/
└── src/main/java/com/baulsanitario/
    ├── ui/
    │   ├── screens/          # Composables de cada pantalla
    │   ├── components/       # Componentes reutilizables
    │   └── viewmodels/       # ViewModels por feature
    ├── domain/
    │   ├── model/            # Entidades del dominio
    │   ├── repository/       # Interfaces de repositorio
    │   └── usecase/          # Casos de uso
    └── data/
        ├── remote/           # Cliente Supabase, DTOs y mappers
        ├── local/            # Room database, DAOs (opcional)
        └── repository/       # Implementaciones de repositorio
```

## Plan de implementación

### Fase 0 — Preparación del ambiente ✓ Completada

Repositorio Git inicializado, README.md y .gitignore creados, sistema de memoria y sesiones con IA configurado.

### Fase 1 — Creación del proyecto Android y configuración de dependencias

1. Crear el proyecto en Android Studio: plantilla Empty Activity, lenguaje Kotlin, Compose habilitado, `minSdk 26`, `targetSdk 36`.
2. Configurar `libs.versions.toml` con las versiones de Compose BOM, Kotlin, ML Kit Document Scanner, Supabase SDK, Ktor, Coroutines y Serialization.
3. Aplicar el plugin `kotlin-serialization` en el `build.gradle.kts` raíz y en el del módulo `app`.
4. Declarar todas las dependencias en el `build.gradle.kts` del módulo `app` usando el version catalog.
5. Agregar `SUPABASE_URL` y `SUPABASE_ANON_KEY` en `local.properties` y exponerlos al código mediante `buildConfigField`.
6. Habilitar `buildConfig = true` en el bloque `buildFeatures` del `build.gradle.kts`.
7. Verificar compilación limpia antes de avanzar.

### Fase 2 — Estructura de paquetes y clase Application

1. Crear la estructura de paquetes siguiendo la arquitectura limpia: `ui/screens`, `ui/components`, `ui/viewmodels`, `domain/model`, `domain/repository`, `domain/usecase`, `data/remote`, `data/local`, `data/repository`.
2. Crear la clase `BaulSanitarioApp : Application()` vacía en el paquete raíz.
3. Registrar `BaulSanitarioApp` en el `AndroidManifest.xml` mediante `android:name`.
4. Crear el objeto `AppContainer` dentro de la Application para centralizar la creación manual de dependencias.

### Fase 3 — Modelos de dominio y contratos de repositorio

1. Crear el enum `DocumentType` con los valores `RECIPE`, `EXAM`, `RECEIPT`, `ORDER` y su nombre legible en español asociado.
2. Crear la entidad `Profile(id: String, name: String, createdAt: Instant)`.
3. Crear la entidad `Document(id: String, profileId: String, type: DocumentType, filePath: String, fileName: String, createdAt: Instant)`.
4. Crear la interfaz `ProfileRepository` en `domain/repository`: `suspend fun getProfiles(): Result<List<Profile>>`.
5. Crear la interfaz `DocumentRepository` en `domain/repository`: `suspend fun getDocumentsByProfile(profileId: String): Result<List<Document>>` y `suspend fun uploadDocument(profileId: String, type: DocumentType, pdfUri: Uri): Result<Document>`.

### Fase 4 — Backend Supabase (infraestructura en la nube)

1. Crear el proyecto en el panel de Supabase y obtener la URL y la `anon key`.
2. Crear la tabla `profiles`: `id uuid PRIMARY KEY DEFAULT gen_random_uuid()`, `name text NOT NULL`, `created_at timestamptz NOT NULL DEFAULT now()`.
3. Crear la tabla `documents`: `id uuid PK`, `profile_id uuid NOT NULL REFERENCES profiles(id)`, `type text NOT NULL`, `file_path text NOT NULL`, `file_name text NOT NULL`, `created_at timestamptz NOT NULL DEFAULT now()`.
4. Habilitar Row Level Security (RLS) en ambas tablas.
5. Crear políticas RLS de lectura y escritura para el usuario autenticado en ambas tablas.
6. Crear el bucket `medical-documents` como privado en Supabase Storage.
7. Crear las políticas de acceso al bucket para el usuario autenticado (SELECT, INSERT, DELETE).
8. Insertar los dos registros iniciales en `profiles`: `"Mamá"` y `"Papá"`.

### Fase 5 — Cliente Supabase e inicialización

1. Agregar el permiso `INTERNET` en `AndroidManifest.xml`.
2. Crear `SupabaseClientProvider` en `data/remote`: instancia única del cliente usando `createSupabaseClient(url, key)` con los plugins `Postgrest`, `Storage` y `Auth` instalados.
3. Inicializar el cliente en `BaulSanitarioApp` y almacenarlo en el `AppContainer`.
4. Verificar conectividad con una lectura simple sobre la tabla `profiles` y loguear el resultado.

### Fase 6 — Autenticación

1. Crear la interfaz `AuthRepository` en `domain/repository`: `suspend fun login(email: String, password: String): Result<Unit>`, `suspend fun logout(): Result<Unit>`, `fun isLoggedIn(): Boolean`.
2. Crear `SupabaseAuthDataSource` en `data/remote` usando el plugin `Auth` del cliente.
3. Implementar `AuthRepositoryImpl` en `data/repository`.
4. Crear los casos de uso `LoginUseCase` y `LogoutUseCase` en `domain/usecase`.
5. Crear `AuthViewModel` con StateFlow de estados: `Idle`, `Loading`, `Success`, `Error(message: String)`.
6. Crear `LoginScreen` en Compose: campos de email y contraseña, botón de ingreso, mensaje de error visible.
7. Configurar la navegación raíz: al iniciar la app verificar si hay sesión activa → si existe navegar a `HomeScreen`; si no, a `LoginScreen`.

### Fase 7 — Captura de documentos con ML Kit Document Scanner

1. Declarar el permiso `CAMERA` en `AndroidManifest.xml`.
2. Implementar la solicitud del permiso de cámara en tiempo de ejecución con `rememberLauncherForActivityResult(RequestPermission)`.
3. Configurar `GmsDocumentScannerOptions`: modo `SCANNER_MODE_FULL`, formato `RESULT_FORMAT_PDF`.
4. Crear el lanzador del escáner con `GmsDocumentScanning.getClient(options)` y `getStartScanIntent`.
5. Crear el caso de uso `ScanDocumentUseCase` que encapsule la inicialización del scanner client y la extracción del URI del PDF del resultado.
6. Crear `ScanDocumentViewModel` con StateFlow de estados: `Idle`, `RequestingPermission`, `Scanning`, `Success(pdfUri: Uri)`, `Error(message: String)`.
7. Crear `ScanScreen` en Compose: botón de escaneo, gestión del permiso en pantalla y vista de preconfirmación del documento capturado.

### Fase 8 — Capa de datos: implementaciones de Storage y base de datos

1. Crear `ProfileDto` y `DocumentDto` en `data/remote` con anotaciones `@Serializable`.
2. Crear las funciones de mapeo `ProfileDto.toDomain()` y `DocumentDto.toDomain()`.
3. Implementar `SupabaseStorageDataSource`: función `uploadPdf(profileId, type, pdfUri)` que sube el PDF al path `profileId/type/uuid.pdf` y retorna el path almacenado.
4. Implementar `SupabaseDatabaseDataSource`: funciones `getProfiles()`, `getDocumentsByProfile(profileId)`, `insertDocument(dto)`.
5. Implementar `DocumentRepositoryImpl` con la lógica transaccional: llamar a `uploadPdf` → si exitoso, llamar a `insertDocument`; si falla la subida, retornar error sin tocar la base de datos.
6. Implementar `ProfileRepositoryImpl`.
7. Registrar ambas implementaciones en el `AppContainer`.
8. Crear `UploadDocumentUseCase`, `GetDocumentsByProfileUseCase` y `GetProfilesUseCase` en `domain/usecase`.

### Fase 9 — Navegación y pantallas principales

1. Configurar `NavHost` con todas las rutas: `login`, `home`, `documentList/{profileId}`, `scan/{profileId}`, `documentDetail/{documentId}`.
2. Crear `HomeViewModel` que consume `GetProfilesUseCase` y expone perfiles mediante StateFlow.
3. Crear `HomeScreen`: lista de perfiles como cards, estados de carga y error.
4. Crear `DocumentListViewModel` que consume `GetDocumentsByProfileUseCase` para el `profileId` recibido como parámetro de navegación.
5. Crear `DocumentListScreen`: lista de documentos con nombre, tipo y fecha; botón flotante que navega a `ScanScreen`.
6. Crear `DocumentDetailScreen`: abre el PDF con un `Intent` hacia el visor del sistema.

### Fase 10 — Flujo completo de carga y pulido

1. Integrar `ScanScreen` en el flujo: al obtener el URI del PDF mostrar un selector de `DocumentType` y el botón de confirmar.
2. Crear `UploadDocumentViewModel` que consume `UploadDocumentUseCase` y expone los estados `Idle`, `Uploading`, `Success`, `Error`.
3. Al finalizar la subida exitosa navegar de vuelta a `DocumentListScreen` y refrescar la lista.
4. Verificar que todos los estados `Loading`, `Empty`, `Error` y `Content` estén implementados visualmente en todas las pantallas.
5. Asegurar que la sesión expirada redirija automáticamente a `LoginScreen`.

### Fase 11 — Pruebas en dispositivo y cierre

1. Instalar la app en dispositivo físico Android.
2. Probar el flujo completo: login → perfiles → seleccionar perfil → escanear → elegir tipo → confirmar → ver en lista → abrir.
3. Probar el flujo de error: cortar internet durante la subida y verificar que no queden inconsistencias en la base de datos.
4. Validar la apertura y visualización del PDF desde el visor del sistema.
5. Ajustar cualquier comportamiento inesperado y cerrar la fase.

## Estado actual

Fase 1 en progreso — pasos 1 al 6 completados. Proyecto Android `BaulSanitario` creado en Android Studio Quail 2026.1.1 con Kotlin y Jetpack Compose. Stack completo declarado en `libs.versions.toml` y `app/build.gradle.kts` (Supabase 3.3.0, Ktor 3.3.0, ML Kit Document Scanner 16.0.0, Coroutines 1.11.0, Navigation Compose 2.8.9). Pendiente: verificación de compilación limpia (paso 7 de Fase 1).

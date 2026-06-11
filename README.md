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

## Plan de acción por fases

### Fase 1 — Configuración inicial del proyecto

1. Crear el proyecto Android en Android Studio con soporte para Kotlin y Jetpack Compose.
2. Configurar el archivo `build.gradle` con todas las dependencias necesarias: Compose BOM, ML Kit Document Scanner, Supabase SDK (kotlinx-serialization, ktor-client-android, postgrest-kt, storage-kt), Coroutines y Room.
3. Inicializar el cliente de Supabase en la aplicación con las variables de entorno (URL y anon key) gestionadas mediante un archivo `local.properties` excluido del repositorio.
4. Crear la estructura de paquetes base siguiendo la arquitectura limpia descrita: `ui`, `domain` y `data` con sus subcarpetas.
5. Verificar la compilación limpia del proyecto vacío antes de avanzar.

### Fase 2 — Captura y procesamiento local con ML Kit

1. Agregar el permiso de cámara en el `AndroidManifest.xml` y gestionar su solicitud en tiempo de ejecución.
2. Implementar el lanzador de la Document Scanner API de ML Kit, configurando las opciones de escaneo (modo completo, límite de páginas, formato PDF).
3. Crear el caso de uso `ScanDocumentUseCase` en la capa de dominio que encapsule la lógica de inicio del escáner y la recuperación del URI del PDF resultante.
4. Crear la pantalla Compose de captura con el ViewModel correspondiente, integrando el lanzador del escáner y mostrando una vista previa del documento capturado.
5. Implementar el manejo de errores para los estados de cancelación y fallo del escáner, propagándolos correctamente hasta la UI mediante StateFlow.

### Fase 3 — Modelamiento de datos y subida a Supabase

1. Diseñar el esquema relacional en Supabase: tabla `profiles` (id, name, created_at) y tabla `documents` (id, profile_id, type, file_path, file_name, created_at).
2. Configurar el bucket de Supabase Storage con las políticas de acceso adecuadas para el usuario autenticado.
3. Implementar `SupabaseStorageDataSource` en la capa de datos con la función de subida del PDF al path jerárquico `perfil_id/tipo_documento/archivo.pdf`.
4. Implementar `SupabaseDatabaseDataSource` con las operaciones de inserción y consulta sobre las tablas `profiles` y `documents`.
5. Implementar el repositorio `DocumentRepository` haciendo cumplir la consistencia transaccional: primero subir el archivo al Storage y solo si la subida es exitosa insertar el registro en la base de datos.
6. Crear los casos de uso `UploadDocumentUseCase` y `GetDocumentsByProfileUseCase` en la capa de dominio.

### Fase 4 — Interfaz de usuario completa

1. Crear la pantalla principal con la lista de perfiles familiares (mamá y papá), navegando al listado de documentos de cada perfil al seleccionarlo.
2. Crear la pantalla de listado de documentos por perfil, mostrando miniaturas o ítems con nombre, tipo y fecha de cada documento.
3. Crear la pantalla de detalle del documento con un visor de PDF embebido o lanzador del visor del sistema.
4. Integrar el flujo completo de captura desde la pantalla de listado: botón de nuevo documento → escáner ML Kit → selección de tipo → subida → confirmación visual.
5. Implementar la navegación entre pantallas con Jetpack Navigation Compose.
6. Pulir los estados de carga, error y vacío en todas las pantallas con feedback visual apropiado al usuario.

## Estado actual

Fase 0 completada — Repositorio base limpio inicializado, estructura de carpetas de trabajo con IA configurada y plan de arquitectura técnica inicialmente definido. Listo para comenzar la implementación de la Fase 1.

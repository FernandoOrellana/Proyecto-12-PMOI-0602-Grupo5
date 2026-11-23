#GRUPO NUMERO 5

José Fernando Orellana Reyes (202210010352)

Carlos Roberto Cardenas Martínez (202430010599)

Jennifer Mariela Marquez Chacon (202330010411)

Michael Abad Diaz Castro (202310040165)

Fernando Abad Diaz Castro (202410040257)

Romel Donaldo Barahona Márquez (202410010359)

##
#


# 🐾 PetCare HN - Sistema de Gestión Veterinaria (Huellitas)
> **Rama de desarrollo**: `Prueba_Local`  
> **Módulos clave**: 
> 1. Historial Completo de Citas por Mascota (Trazabilidad clínica para Administrador y Veterinario).
> 2. Registro y Actualización de Observaciones Clínicas por el Veterinario.  
> **Arquitectura**: Full-Stack (.NET 10 Web API + Android Kotlin con Jetpack Compose MVVM + MySQL Remoto).

---

## 📑 Tabla de Contenidos
1. [Visión General del Sistema](#-visión-general-del-sistema)
2. [Requisitos Previos de Entorno](#-requisitos-previos-de-entorno)
3. [Parámetros de Configuración (Placeholders)](#-parámetros-de-configuración-placeholders)
4. [Paso 1: Configurar y Levantar la Base de Datos](#-paso-1-configurar-y-levantar-la-base-de-datos)
5. [Paso 2: Configurar y Levantar el Backend (.NET 10 API)](#-paso-2-configurar-y-levantar-el-backend-net-10-api)
6. [Paso 3: Configurar y Ejecutar la App Móvil (Android)](#-paso-3-configurar-y-ejecutar-la-app-móvil-android)
7. [Documentación de Endpoints Implementados](#-documentación-de-endpoints-implementados)
8. [Matriz de Roles y Permisos](#-matriz-de-roles-y-permisos)
9. [Guía de Solución de Problemas (Troubleshooting)](#-guía-de-solución-de-problemas-troubleshooting)

---

## 🎯 Visión General del Sistema

El sistema permite gestionar la atención clínica de pacientes de la veterinaria con las siguientes reglas de negocio:
* **Trazabilidad clínica total**: Las citas registradas nunca se borran físicamente (`citas`), conservando sus estados: `PENDIENTE`, `CONFIRMADA`, `ATENDIDA` y `CANCELADA`.
* **Historial compartido**: Tanto el **Administrador** como **cualquier Veterinario** pueden auditar el historial clínico de cualquier mascota sin restricciones de autoría.
* **Observaciones clínicas dinámicas**: Al consultar el historial o atender citas, el veterinario puede agregar o editar observaciones clínicas directamente sobre cada cita/consulta (`consultas.observaciones`).

---

## 💻 Requisitos Previos de Entorno

Asegúrate de contar con las siguientes herramientas instaladas en tu equipo de desarrollo:

* **.NET SDK**: Versión `10.0` o superior ([Descargar .NET](https://dotnet.microsoft.com/download)).
* **Android Studio**: Versión `Ladybug` / `Meerkat` o superior ([Descargar Android Studio](https://developer.android.com/studio)).
* **Java SDK (JDK)**: JBR 21 (incluido dentro del directorio de instalación de Android Studio).
* **Cliente SQL**: [Antares SQL](https://antares-sql.app/downloads), DBeaver o MySQL Workbench.
* **Dispositivo Android / Emulador**: API 26 en adelante (recomendado: Emulador Android API 34 o 35 con 4 GB de RAM).

---

## 🔑 Parámetros de Configuración (Placeholders)

> **Instrucciones para el desarrollador**: Reemplaza los siguientes `<PLACEHOLDERS>` con los valores asignados a tu máquina o entorno:

| Variable de Configuración | Descripción del Placeholder | Valor sugerido / Ejemplo |
| :--- | :--- | :--- |
| `<TU_IP_LOCAL>` | Dirección IPv4 de tu PC en la red local (obtenida con `ipconfig`) | `192.168.100.15` |
| `<PUERTO_API>` | Puerto TCP donde escucha la Web API en tu máquina | `5000` |
| `<HOST_MYSQL>` | Dirección IP o Hostname del servidor de base de datos MySQL | `170.231.53.218` |
| `<PUERTO_MYSQL>` | Puerto del servidor MySQL | `510` |
| `<NOMBRE_BD>` | Nombre de la base de datos | `MPF` |
| `<USUARIO_MYSQL>` | Usuario de conexión a MySQL | `GRUPO5` |
| `<PASSWORD_MYSQL>` | Contraseña del usuario de MySQL | `011081` |
| `<JWT_SECRET>` | Clave secreta para generación y firma de tokens JWT | *(Cadena de al menos 32 caracteres)* |
| `<EMAIL_ADMIN>` | Correo para inicio de sesión de Administrador | `admin@huellitas.hn` |
| `<PASSWORD_ADMIN>` | Contraseña de Administrador | `Admin12345*` |
| `<EMAIL_VET>` | Correo para inicio de sesión de Veterinario | `veterinario@demo.hn` |
| `<PASSWORD_VET>` | Contraseña de Veterinario | `Demo12345` |

---

## 🗄️ Paso 1: Configurar y Levantar la Base de Datos

1. Abre tu gestor SQL favorito (ej. **Antares SQL**).
2. Crea una nueva conexión con los siguientes parámetros:
   * **Host**: `<HOST_MYSQL>` (ej. `170.231.53.218`)
   * **Port**: `<PUERTO_MYSQL>` (ej. `510`)
   * **Database**: `<NOMBRE_BD>` (ej. `MPF`)
   * **Username**: `<USUARIO_MYSQL>` (ej. `GRUPO5`)
   * **Password**: `<PASSWORD_MYSQL>` (ej. `011081`)
3. Conéctate y verifica que existan las tablas principales: `usuarios`, `roles`, `mascotas`, `citas` y `consultas`.

---

## ⚙️ Paso 2: Configurar y Levantar el Backend (.NET 10 API)

### 1. Configurar `appsettings.json`
Edita el archivo `api-net/src/PetCare.Api/appsettings.json` y define tus credenciales:

```json
{
  "ConnectionStrings": {
    "MySql": "Server=<HOST_MYSQL>;Port=<PUERTO_MYSQL>;Database=<NOMBRE_BD>;User ID=<USUARIO_MYSQL>;Password=<PASSWORD_MYSQL>;SslMode=Preferred;"
  },
  "Jwt": {
    "Secret": "<JWT_SECRET>",
    "AccessMinutes": 30
  },
  "Seed": {
    "AdminEmail": "<EMAIL_ADMIN>",
    "AdminPassword": "<PASSWORD_ADMIN>",
    "DemoPassword": "<PASSWORD_VET>"
  }
}
```

### 2. Restaurar, Probar y Correr la API
Desde una consola de comandos en la raíz del proyecto (`PMO-0602`):

```powershell
# 1. Restaurar paquetes NuGet
dotnet restore api-net/PetCare.sln

# 2. Ejecutar la suite completa de pruebas unitarias (38 tests)
dotnet test api-net/tests/PetCare.Tests/PetCare.Tests.csproj

# 3. Iniciar la API escuchando en todas las interfaces de red locales
dotnet run --project api-net/src/PetCare.Api --urls "http://0.0.0.0:<PUERTO_API>"
```

> **Verificación**: Abre en tu navegador `http://localhost:<PUERTO_API>/api-docs` para explorar la documentación interactiva de Swagger.

---

## 📱 Paso 3: Configurar y Ejecutar la App Móvil (Android)

### 1. Configurar la URL Base de la API
1. En Windows, obtén tu IP local ejecutando `ipconfig` (busca el adaptador conectado a tu red Wi-Fi o Ethernet).
2. Abre el archivo `app/build.gradle.kts` y actualiza la constante `API_BASE_URL`:

```kotlin
// Reemplaza <TU_IP_LOCAL> y <PUERTO_API> con tus valores:
buildConfigField("String", "API_BASE_URL", "\"http://<TU_IP_LOCAL>:<PUERTO_API>\"")
// Ejemplo: buildConfigField("String", "API_BASE_URL", "\"http://192.168.100.15:5000\"")
```

### 2. Configuración Recomendada del Emulador (Android Virtual Device)
Para evitar que el emulador se congele en el inicio:
* En **Android Studio** ve a: *Tools* > *Device Manager*.
* Edita tu AVD (ej. Pixel) > *Show Advanced Settings*:
  * **RAM**: `4096 MB` (mínimo recomendado para imágenes API 34+).
  * **VM Heap**: `512 MB`.

### 3. Compilar e Instalar el APK

```powershell
# 1. Configurar JAVA_HOME si no está en las variables globales de Windows
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

# 2. Compilar el proyecto Android
.\gradlew.bat assembleDebug

# 3. Mapear el puerto en ADB (opcional si usas emulador o cable USB)
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb reverse tcp:<PUERTO_API> tcp:<PUERTO_API>

# 4. Instalar y abrir la app
& $adb install -r "app/build/outputs/apk/debug/app-debug.apk"
& $adb shell am start -n com.uth.proyectofinal/.MainActivity
```

---

## 📡 Documentación de Endpoints Implementados

### 1. Historial de Citas por Mascota
* **Ruta**: `GET /api/mascotas/{id}/citas`
* **Autorización**: `Bearer Token` (`ADMINISTRADOR`, `VETERINARIO` o `DUENO` de la mascota).
* **Descripción**: Devuelve todas las citas de la mascota ordenadas cronológicamente de forma descendente, incluyendo estado, servicio, dueño, veterinario asignado y detalles de la consulta si fue atendida.
* **Respuesta exitosa (`200 OK`)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 7,
      "fecha": "2027-04-28",
      "hora": "08:00",
      "estado": "ATENDIDA",
      "servicio": { "id": 1, "nombre": "Consulta general", "precio": 350.00 },
      "veterinario": { "id": 2, "nombre": "Dra. Ana Martínez" },
      "dueno": { "id": 4, "nombre": "Cliente Demo" },
      "consulta": {
        "id": 3,
        "diagnostico": "Sano",
        "tratamiento": "Ninguno",
        "observaciones": "Mascota en excelente estado post-tratamiento.",
        "peso": 25.4
      }
    }
  ]
}
```

### 2. Agregar / Editar Observación Clínica
* **Ruta**: `PATCH /api/citas/{id}/observaciones`
* **Autorización**: `Bearer Token` (`ADMINISTRADOR` o `VETERINARIO`).
* **Descripción**: Permite a cualquier veterinario o administrador agregar o actualizar observaciones clínicas en una cita del historial. Si la cita ya tiene consulta, actualiza `consultas.observaciones`; si aún no la tenía, crea la consulta clínica automáticamente.
* **Cuerpo de la petición (`Body`)**:
```json
{
  "observaciones": "Mascota presentó leve deshidratación, se administraron sueros de rehidratación."
}
```
* **Respuesta exitosa (`200 OK`)**:
```json
{
  "success": true,
  "data": {
    "id": 3,
    "citaId": 7,
    "diagnostico": "Consulta médica",
    "observaciones": "Mascota presentó leve deshidratación, se administraron sueros de rehidratación."
  }
}
```

---

## 🛡️ Matriz de Roles y Permisos

| Funcionalidad | Dueño (`DUENO`) | Veterinario (`VETERINARIO`) | Administrador (`ADMINISTRADOR`) |
| :--- | :---: | :---: | :---: |
| Ver historial de sus propias mascotas | ✅ Sí | ✅ Sí | ✅ Sí |
| Ver historial de **cualquier** mascota | ❌ No | ✅ Sí | ✅ Sí |
| Ver citas de otros veterinarios | ❌ No | ✅ Sí (Trazabilidad clínica) | ✅ Sí |
| Agregar / Editar Observaciones | ❌ No | ✅ Sí | ✅ Sí |
| Registrar nueva consulta médica | ❌ No | ✅ Sí | ✅ Sí |

---

## 🔧 Guía de Solución de Problemas (Troubleshooting)

### A. "Sin conexión con el servidor. Revisa tu internet" en la app móvil
* **Causa**: Las versiones modernas de Android (API 28+) bloquean por defecto cualquier conexión HTTP sin cifrar (`http://...`).
* **Solución**: 
  1. Verifica que `app/src/main/AndroidManifest.xml` contenga `android:usesCleartextTraffic="true"`.
  2. Verifica que `app/src/main/res/xml/network_security_config.xml` tenga declaradas las IPs locales.
  3. Revisa que `API_BASE_URL` en `app/build.gradle.kts` tenga la IP real de tu PC y que la API esté corriendo en `http://0.0.0.0:5000`.

### B. "InvalidCastException: System.DateTime to System.DateOnly" en .NET
* **Causa**: El driver `MySql.EntityFrameworkCore` mapea las columnas `DATE` de MySQL como `DateTime`, lo que genera incompatibilidad con las entidades C# que usan `DateOnly`.
* **Solución**: En `PetCareDbContext.cs` ya están configurados los `ValueConverter<DateOnly, DateTime>` en `OnModelCreating`. Mantén siempre esos conversores activos.

### C. El emulador inicia congelado o en pantalla negra
* **Causa**: La imagen AVD se quedó sin memoria o el archivo snapshot se corrompió al cerrarse de golpe.
* **Solución**: En Android Studio abre *Device Manager*, haz clic derecho sobre el emulador y selecciona **Cold Boot Now**. Si persiste, elimina la carpeta de snapshots temporales en `~/.android/avd/<NOMBRE_AVD>.avd/snapshots/default_boot/`.

### D. Error 401 Unauthorized al iniciar sesión
* **Causa**: La contraseña en la base de datos remota difiere de la esperada por el código.
* **Solución**: Usa las credenciales sincronizadas (`Admin12345*` para admin y `Demo12345` para veterinarios y dueños demo). Si creas usuarios nuevos, asegúrate de que el campo `email_verificado` esté en `1` en la tabla `usuarios`.

# Pruebas y documentación - PetCare HN

**Autor:** Fernando (integrante de pruebas y documentación)
**Rama:** Fernando_Pruebas

## Resumen de resultados

| Área | Herramienta | Resultado |
|---|---|---|
| Pruebas unitarias | xUnit + Moq (ASP.NET Core, .NET 10) | 63 de 63 correctas, 0 errores (repo final) |
| Documentación de la API | Swagger / OpenAPI | Login, historial de citas, observaciones clínicas y control de roles probados |
| Pruebas de endpoints | Postman | 49 pruebas pasadas, 0 fallidas, contra el servidor desplegado |
| App móvil | Android Studio (emulador) | Login, historial de citas, observaciones y cobro de pedidos |
| Control de versiones | Git y GitHub | Rama Fernando_Pruebas y Pull Request a main |

Nota: las capturas 01, 02a y 02b corresponden a una corrida anterior de 42 pruebas (rama Prueba_LocalV2). La captura 14 es la corrida final de 63 pruebas sobre este repositorio, que incluye los módulos de cobros y despacho.

## Capturas (carpeta `Imagenes prueba`)

| Archivo | Descripción |
|---|---|
| 01_pruebas_unitarias_resumen.png | Resumen de la primera corrida de pruebas unitarias (42) |
| 02a_pruebas_unitarias_detalle.png | Detalle de las pruebas, primera parte |
| 02b_pruebas_unitarias_resumen.png | Detalle de las pruebas y resumen final (42) |
| 03_swagger_endpoints.png | Swagger con la lista de endpoints |
| 04_swagger_login_admin.png | Login de administrador, respuesta 200 |
| 05_api_corriendo.png | API local escuchando en el puerto 5000 |
| 06_base_de_datos.png | Base de datos MPF en Antares (tabla citas) |
| 07_postman_resultados.png | Postman: primera colección (28 pruebas pasadas) |
| 08_swagger_historial.png | GET /api/mascotas/1/citas, respuesta 200 |
| 09_swagger_observaciones.png | PATCH /api/citas/1/observaciones, respuesta 200 |
| 10_swagger_403_dueno.png | Un dueño no puede editar observaciones: 403 |
| 10a_swagger_login_dueno.png | Login del dueño usado en la prueba anterior |
| 11_app_login.png | App: pantalla de inicio de sesión |
| 11b_app_error_conexion_local.png | App: error de conexión al probar con la API local desde el emulador |
| 11c_app_panel_admin.png | App: panel del administrador (productos) |
| 12_app_historial.png | App: historial de citas |
| 12b_app_carne_historial.png | App: carné e historial de la mascota |
| 13_app_observaciones.png | App: observación clínica editada en el historial de Firulais |
| 14_pruebas_unitarias_repo_nuevo.png | Pruebas unitarias del repo final: 63 correctas |
| 15_app_cobro.png | App: pedido #20 con el comprobante interno COM-17 |
| 15b_app_pedido_entregado.png | App: el mismo pedido marcado como entregado |
| 16_postman_cobros.png | Postman: colección ampliada con cobros (49 pruebas pasadas) |

## Colecciones de Postman

- `PetCare_HN_Pruebas.postman_collection.json`: primera versión (22 solicitudes): autenticación, historial de citas, observaciones clínicas y autorización por rol.
- `PetCare_HN_Pruebas_v2.postman_collection.json`: versión final (40 solicitudes). Incluye todo lo anterior y agrega los endpoints de cobros: permisos, validaciones, recursos inexistentes y un flujo de cobro real.

La carpeta 6 de la versión final escribe en la base de datos. Para no modificar datos, desmarcarla en el Collection Runner.

## Cómo reproducir

Pruebas unitarias (desde la raíz del repositorio):

```powershell
dotnet test api-net/tests/PetCare.Tests/PetCare.Tests.csproj
```

API local:

```powershell
dotnet run --project api-net/src/PetCare.Api --urls "http://0.0.0.0:5000"
```

Swagger: `http://localhost:5000/api-docs`

Postman: importar el archivo `.json`, ajustar la variable `baseUrl` (por defecto `http://localhost:5000`) y ejecutar con Run collection.

## Usuarios de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | admin@huellitas.hn | Admin12345* |
| Veterinario | veterinario@demo.hn | Demo12345 |
| Dueño | dueno@demo.hn | Demo12345 |

## Observaciones

- Las pruebas de Postman y de la app se ejecutaron contra el servidor desplegado.
- Al probar la app con la API local desde el emulador hubo un error de conexión (captura 11b); se resolvió usando el servidor desplegado.
- La compilación muestra advertencias de paquetes con vulnerabilidades conocidas (MailKit, MimeKit, System.Security.Cryptography.Pkcs) y de métodos obsoletos de Firebase. No afectan los resultados de las pruebas y quedan como mejora pendiente.

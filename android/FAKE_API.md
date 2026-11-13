# API falsa para desarrollo (debug)

Permite ejecutar la app Android **sin backend**. Sirve para construir y probar pantallas mientras la API real no está disponible. No hay que levantar Docker, Spring Boot ni ningún servidor.

## Cómo funciona

Toda la app habla con el servidor a través de la interfaz `CocinaVecinaApi` (Retrofit). La API falsa, `FakeCocinaVecinaApi`, implementa esa misma interfaz con datos en memoria. `AppContainer` elige cuál usar mediante `ApiProvider`:

| Versión | Qué usa |
|---|---|
| debug con `cocinavecina.useFakeApi=true` | API falsa |
| debug sin esa propiedad | API real (`http://10.0.2.2:8080`) |
| release | API real, siempre |

Las pantallas no se enteran del cambio: siguen llamando a `repository.call { ... }` como siempre. El código de la API falsa vive solo en `app/src/debug`, así que **nunca entra en el APK de release**.

## Cómo activarla

1. Abre `android/local.properties`. Este archivo es local y no se sube a git.
2. Agrega esta línea:
   ```properties
   cocinavecina.useFakeApi=true
   ```
3. Sincroniza Gradle (*Sync Project with Gradle Files* en Android Studio).
4. Comprueba en *Build Variants* que la variante activa sea **debug** y ejecuta la app en un emulador.

También se puede activar solo para una compilación:
```bash
cd android
sh ./gradlew installDebug -Pcocinavecina.useFakeApi=true
```

Para volver a la API real, quita la línea o ponla en `false` y sincroniza de nuevo.

**Cómo saber si está activa:** el login tarda unos 400 ms en responder, porque la API falsa simula la latencia de la red. Si aparece "Sin conexión", la app está intentando usar el backend real y la línea de `local.properties` no se aplicó.

## Cuentas de prueba

La contraseña de todas es `una-clave-larga`.

| Correo | Rol | Para probar |
|---|---|---|
| `comprador@example.com` | Comprador | Explorar, pedir porciones, negociar el precio, pedidos |
| `vendedora@example.com` | Vendedora | Publicar, responder solicitudes, marcar pedidos como listos, validar entregas |
| `moderador@example.com` | Moderador | Cola de ofertas en revisión |
| `sinverificar@example.com` | Comprador sin verificar | El error de cuenta sin verificar al iniciar sesión |

También puedes crear una cuenta desde la pantalla de registro. En la API falsa queda verificada al instante, así que puedes iniciar sesión con ella enseguida.

## Qué simula

Todos los endpoints de `CocinaVecinaApi` tienen respuesta:

- **Cuenta:** registro (con rol, mayoría de edad y términos) e inicio de sesión.
- **Ofertas:** búsqueda cercana, filtrada por radio y ordenada por distancia, precio o vencimiento; detalle; publicar; puntos de retiro.
- **Solicitudes de compra:** enviar, contraofertar y proponer por turnos, aceptar y rechazar. Máximo de 3 rondas y precio entre el mínimo y el base.
- **Pedidos:** generar código de retiro, marcar como listo, validar la entrega con el código, cancelar y ver los datos de retiro.
- **Moderación:** reportar una oferta, ver la cola y reactivar o retirar.
- **Dispositivos:** registro del token de notificaciones (no hace nada).

Los errores llegan con códigos HTTP reales (401, 403, 404, 409, 422) y un cuerpo de error, así que la app muestra los mismos mensajes que mostraría con el backend.

Datos que ya existen al abrir la app:
- 4 ofertas activas y 1 oculta en revisión, todas de `vendedora@example.com`.
- 1 solicitud de compra pendiente del comprador.
- 1 pedido aceptado entre el comprador y la vendedora.

## Limitaciones

- **Los datos viven en memoria.** Al cerrar la app se pierden las cuentas registradas y lo publicado. Las cuentas de prueba siempre existen.
- **Sesión huérfana.** Si cerraste la app con una cuenta registrada, la sesión sigue guardada pero el usuario ya no existe: la app te pedirá iniciar sesión de nuevo.
- **No se pueden subir fotos.** `prepareUpload` responde un error 501. Publica sin foto.
- **Tiempo real no incluido.** La conexión WebSocket de eventos sigue intentando llegar al servidor real y falla en silencio. La API falsa no envía notificaciones ni eventos.
- **Ubicación.** Las ofertas están cerca del centro de Concepción (-36.8270, -73.0503). El emulador suele ubicarse en California; en *Extended controls* → *Location* pon esas coordenadas, o no actives el permiso de ubicación.
- **Reglas aproximadas.** Las reglas de negocio son una aproximación y pueden diferir del backend real. No sirve para validar el contrato con la API.

## Cómo agregar o cambiar datos

- **Datos de ejemplo** (usuarios, ofertas, puntos de retiro, cola de moderación): `app/src/debug/java/cl/udec/cocinavecina/data/fake/FakeSeed.kt`.
- **Comportamiento de un endpoint:** `app/src/debug/java/cl/udec/cocinavecina/data/fake/FakeCocinaVecinaApi.kt`. Cada función corresponde a una de `CocinaVecinaApi`.
- **Latencia simulada:** el parámetro `latencyMillis` del constructor (400 ms por defecto). Redúcelo a `0` si te estorba.

## Si se agrega un endpoint a `CocinaVecinaApi`

La API falsa deja de compilar hasta que lo implementes, porque Kotlin exige implementar todas las funciones de la interfaz. Agrégalo en `FakeCocinaVecinaApi.kt`:

- Usa `respond { ... }` para simular la latencia y proteger el estado compartido.
- Usa `fail(código, "mensaje")` para devolver errores.
- Usa `requireUser()` si el endpoint necesita sesión iniciada.

## Archivos

| Archivo | Función |
|---|---|
| `app/src/debug/.../data/fake/FakeCocinaVecinaApi.kt` | La API falsa |
| `app/src/debug/.../data/fake/FakeSeed.kt` | Datos de ejemplo |
| `app/src/debug/.../data/ApiProvider.kt` | En debug, elige entre la API falsa y la real |
| `app/src/release/.../data/ApiProvider.kt` | En release, siempre la real |
| `app/src/main/.../AppContainer.kt` | Usa `ApiProvider.create(...)` |
| `app/build.gradle.kts` | Lee `cocinavecina.useFakeApi` y genera `BuildConfig.USE_FAKE_API` |

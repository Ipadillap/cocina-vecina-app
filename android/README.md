# App Android

Abrir esta carpeta en Android Studio y ejecutar la configuración `app`.

- URL de la API: `cocinavecina.apiUrl` en `gradle.properties` (emulador: `http://10.0.2.2:8080`; servidor:
  `https://<dominio>`). En release solo se permite HTTPS (`res/xml/network_security_config.xml`).
- Push (FCM): crear el proyecto en la consola de Firebase, descargar `google-services.json` en `app/`
  (no se versiona). Sin ese archivo la app compila y funciona, solo que sin avisos en segundo plano.
- Estructura: `data/` (API en `network/`, sesión en `session/`, WebSocket en `network/RealtimeEvents`),
  `ui/` (una carpeta por pantalla), `AppContainer` (dependencias).
- Pruebas: `./gradlew testDebugUnitTest`.

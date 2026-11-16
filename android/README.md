# App Android

Abrir esta carpeta en Android Studio y ejecutar la configuración `app`.

- **API falsa (debug):** `cocinavecina.useFakeApi=true` en `gradle.properties` hace que la app funcione sin backend
  (ver `FAKE_API.md`). Con `false` usa `cocinavecina.apiUrl` (emulador: `http://10.0.2.2:8080`; servidor:
  `https://<dominio>`). Release siempre usa la API real y solo permite HTTPS (`res/xml/network_security_config.xml`).
- Push (FCM): crear el proyecto en la consola de Firebase, descargar `google-services.json` en `app/`
  (no se versiona). Sin ese archivo la app compila y funciona, solo que sin avisos en segundo plano.
- Estructura: `data/` (API en `network/`, sesión en `session/`, WebSocket en `network/RealtimeEvents`),
  `location/` (GPS a 100 m, grilla de 400 m, geocodificación Nominatim), `ui/` (una carpeta por pantalla),
  `AppContainer` (dependencias).
- Pantallas: explorar (mapa/lista), detalle, proponer precio, seguimiento de solicitud, pedido (código/QR),
  Vender (oferta activa, solicitudes, entregas), publicar en 3 pasos, cuenta, moderación.
- Pruebas: `./gradlew testDebugUnitTest`.

## Para auditar contra la API real

Toda llamada pasa por `Repository.call { ... }` sobre la interfaz `CocinaVecinaApi`, que sigue `docs/api/openapi.yaml`.
Las pantallas no tienen datos de ejemplo: lo falso vive solo en `app/src/debug/.../data/fake`.
Campos que la app lee pero **no están en el contrato** (opcionales, la UI tiene alternativa si faltan):
`NearbyOffer.seller`, `OfferView.totalPortions`, `OfferView.sellerVerified`, `PurchaseRequestView.buyerName`,
`OrderView.buyerName`. Endpoints del contrato aún sin usar en la app: `auth/verify`, `purchase-requests/{id}/withdraw`,
`devices/{pushToken}` (DELETE), `photos/local/{key}`.

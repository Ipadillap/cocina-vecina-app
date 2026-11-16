# Integración front1 + front2 + main (android/)

- Base: rama front2 (capa de datos, API falsa, ViewModels y tests).
- De front1: wizard de publicar en 3 pasos (`ui/publish`), diseño del dashboard del vendedor (`ui/seller`),
  diseño de moderación. Se quitaron todos sus datos de ejemplo dentro de las pantallas.
- De main: `Locator` (T2, ahora con redondeo a 100 m), `Photos` (T3), mapa MapLibre/OpenFreeMap (T1/T5),
  geocodificador Nominatim del PoC T4 (`location/AddressGeocoder`), geometría de celdas (`location/Grid`, T6).
  `poc/` no se toca.
- Simplificación: reglas de publicación en `PublishRules` (puras y con tests); una sola pantalla de pedidos
  (`OrderDetail*`); `RequestsScreen` solo muestra mis solicitudes como comprador.
- No incluido: cámara ("Tomar foto"), dígitos separados al validar el código (se usa un campo + escaneo QR),
  pestañas de derechos del titular y bitácora del servidor en moderación (no existen en el contrato).

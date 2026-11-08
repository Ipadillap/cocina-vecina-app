# ADR 0002 · App nativa Android en Kotlin con Jetpack Compose

**Estado:** aceptada (reemplaza a React Native + Expo).

**Contexto.** El 83,79 % de los móviles en Chile es Android (EV-7) y publicar en App Store cuesta
US$ 99/año (restricción cost). El equipo domina Java/Kotlin, y la API ya es Java.

**Decisión.** Una sola app nativa Android (minSdk 24) en Kotlin con Jetpack Compose, MapLibre Android para el
mapa con teselas OpenFreeMap, Retrofit + kotlinx.serialization para la API, OkHttp para el WebSocket y
Firebase Cloud Messaging para los avisos.

**Consecuencias.** Un solo lenguaje de la familia JVM en todo el equipo, sin capa de compatibilidad ni
servicio intermedio de push (FCM directo). iOS queda fuera del semestre: exigiría una app en Swift o migrar la
interfaz a Compose Multiplatform.

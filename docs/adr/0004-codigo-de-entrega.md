# ADR 0004 · Código de entrega de 6 dígitos con HMAC, 5 intentos, número y QR

**Estado:** aceptada.

**Contexto.** La retroalimentación del Hito 1 dejó el mecanismo para validar la entrega «en consideración».
La regla RB-11 exige que la probabilidad de que un tercero adivine el código sea ≤ 5 · 10⁻⁶.

**Alternativas.**

| Alternativa | P(adivinar) con 5 intentos | Motivo |
|---|---|---|
| Código de 4 dígitos | 5 / 10⁴ = 5 · 10⁻⁴ | 100 veces sobre el objetivo; descartada |
| Código de 6 dígitos | 5 / 10⁶ = 5 · 10⁻⁶ | cumple RB-11 y se dicta en voz alta |
| Solo QR | — | falla si la cámara no funciona o hay poca luz |
| Número + QR | igual que el número | elegida: el QR agiliza y el número es respaldo |

**Decisión.** Código de 6 dígitos generado con `SecureRandom`, guardado solo como HMAC-SHA256
(`OrderService.hmac`), máximo 5 intentos y 2 emisiones por pedido; la app lo muestra como número y como QR
(`QrCode.kt` con ZXing, escaneo con ML Kit en `OrderDetailScreen.kt`).

**Verificación.** `poc/t8-codigo/simulacion_codigo.py` simula 10⁶ ataques de 5 intentos y guarda el
resultado en `poc/t8-codigo/resultados_t8.json` (esperado ≈ 5 aciertos).

**Consecuencias.** Si el código se filtra de la base, el HMAC no se puede invertir sin la clave; a cambio,
el vendedor no puede ver el código, solo validarlo.

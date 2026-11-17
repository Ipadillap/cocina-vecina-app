"""poc/t8-codigo/simulacion_codigo.py
Simula 10^6 ataques de fuerza bruta contra el código de entrega de 6 dígitos
con 5 intentos (RB-11). Cada ataque: código secreto aleatorio y 5 intentos
distintos al azar. Cuenta aciertos y compara con P teórica = 5/10^6.
Uso: python simulacion_codigo.py [ataques] [semilla]
"""
import json
import random
import secrets
import sys

N = int(sys.argv[1]) if len(sys.argv) > 1 else 1_000_000
SEED = int(sys.argv[2]) if len(sys.argv) > 2 else 2026
ESPACIO = 10**6
INTENTOS = 5

rng = random.Random(SEED)  # atacante reproducible
aciertos = 0
for _ in range(N):
    codigo = secrets.randbelow(ESPACIO)  # igual que SecureRandom en OrderService
    intentos = rng.sample(range(ESPACIO), INTENTOS)  # 5 intentos distintos
    if codigo in intentos:
        aciertos += 1

p_teo = INTENTOS / ESPACIO
p_obs = aciertos / N
res = {
    "ataques": N,
    "intentos_por_ataque": INTENTOS,
    "aciertos": aciertos,
    "p_observada": p_obs,
    "p_teorica": p_teo,
    "esperado": N * p_teo,
    "cumple_RB11": p_teo <= 5e-6,
}
print(json.dumps(res, indent=2))
with open("resultados_t8.json", "w") as f:
    json.dump(res, f, indent=2)

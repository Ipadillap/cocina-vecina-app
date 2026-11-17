"""Prueba de concurrencia del stock (RNF-R-4) y comparación de 3 mecanismos.

Escenario: 50 compradores aceptan al mismo tiempo una oferta con 5 porciones
(1 porción cada uno), repetido 100 veces por mecanismo.

  ALT-1  UPDATE condicionado (la consulta real de OfferRepository.reserve)
  ALT-2  bloqueo pesimista: SELECT ... FOR UPDATE y luego UPDATE
  ALT-3  leer y luego escribir sin bloqueo (línea base ingenua)

Requisitos: PostgreSQL con PostGIS y las migraciones de
backend/src/main/resources/db/migration aplicadas. Ejemplo:

  python perf/stock_concurrency.py "host=localhost port=5432 dbname=cocinavecina user=postgres"

Salida: tabla en consola y perf/resultados_stock.json
"""
import json
import statistics
import sys
import threading
import time
from pathlib import Path

import psycopg

DSN = sys.argv[1] if len(sys.argv) > 1 else "host=localhost port=5432 dbname=cocinavecina user=postgres"
BUYERS = int(sys.argv[2]) if len(sys.argv) > 2 else 50
PORTIONS = int(sys.argv[3]) if len(sys.argv) > 3 else 5
REPS = int(sys.argv[4]) if len(sys.argv) > 4 else 100

# Copia literal de OfferRepository.reserve (ALT-1)
RESERVE_SQL = """
update offer
set available_portions = available_portions - %(portions)s,
    status = case when available_portions - %(portions)s = 0 then 'SOLD_OUT' else status end
where id = %(offer_id)s
  and status = 'ACTIVE'
  and expires_at > now()
  and available_portions >= %(portions)s
"""


def alt1_conditional_update(conn, offer_id):
    with conn.transaction():
        return conn.execute(RESERVE_SQL, {"portions": 1, "offer_id": offer_id}).rowcount == 1


def alt2_select_for_update(conn, offer_id):
    with conn.transaction():
        row = conn.execute(
            "select available_portions from offer where id = %s and status = 'ACTIVE' for update",
            (offer_id,),
        ).fetchone()
        if row is None or row[0] < 1:
            return False
        conn.execute(
            "update offer set available_portions = %s,"
            " status = case when %s = 0 then 'SOLD_OUT' else status end where id = %s",
            (row[0] - 1, row[0] - 1, offer_id),
        )
        return True


def alt3_read_then_write(conn, offer_id):
    with conn.transaction():
        row = conn.execute(
            "select available_portions from offer where id = %s and status = 'ACTIVE'", (offer_id,)
        ).fetchone()
    if row is None or row[0] < 1:
        return False
    with conn.transaction():
        conn.execute(
            "update offer set available_portions = %s where id = %s", (row[0] - 1, offer_id)
        )
    return True


MECHANISMS = {
    "ALT-1 UPDATE condicionado": alt1_conditional_update,
    "ALT-2 SELECT FOR UPDATE": alt2_select_for_update,
    "ALT-3 leer y escribir": alt3_read_then_write,
}


def setup_offer(admin):
    """Crea vendedor, punto de retiro y una oferta de prueba. Devuelve el id de la oferta."""
    with admin.transaction():
        seller = admin.execute(
            "insert into app_user (email, display_name, password_hash, role, adult_declared,"
            " verified_at, status, created_at)"
            " values (%s, 'Vendedor prueba', 'x', 'SELLER', true, now(), 'ACTIVE', now())"
            " returning id",
            (f"stock-test-{time.time_ns()}@test.local",),
        ).fetchone()[0]
        point = admin.execute(
            "insert into pickup_point (seller_id, alias, encrypted_address, encrypted_lat,"
            " encrypted_lon, cell_lat, cell_lon, created_at)"
            " values (%s, 'prueba', 'x', 'x', 'x', -36.827, -73.050, now()) returning id",
            (seller,),
        ).fetchone()[0]
        offer = admin.execute(
            "insert into offer (seller_id, pickup_point_id, title, category, base_price, min_price,"
            " total_portions, available_portions, prepared_at, published_at, expires_at, status)"
            " values (%s, %s, 'Prueba de stock', 'MAIN', 3000, 2000, %s, %s,"
            " now(), now(), now() + interval '3 hours', 'ACTIVE') returning id",
            (seller, point, PORTIONS, PORTIONS),
        ).fetchone()[0]
    return offer, point, seller


def reset_offer(admin, offer_id):
    with admin.transaction():
        admin.execute(
            "update offer set available_portions = %s, status = 'ACTIVE' where id = %s",
            (PORTIONS, offer_id),
        )


def run_round(conns, mechanism, offer_id):
    barrier = threading.Barrier(len(conns))
    results = [None] * len(conns)
    latencies = [0.0] * len(conns)

    def worker(i):
        barrier.wait()
        t0 = time.perf_counter()
        ok = mechanism(conns[i], offer_id)
        latencies[i] = (time.perf_counter() - t0) * 1000
        results[i] = ok

    threads = [threading.Thread(target=worker, args=(i,)) for i in range(len(conns))]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    return sum(results), latencies


def percentile(values, p):
    ordered = sorted(values)
    k = max(0, min(len(ordered) - 1, round(p / 100 * len(ordered) + 0.5) - 1))
    return ordered[k]


def main():
    admin = psycopg.connect(DSN, autocommit=True)
    offer_id, point_id, seller_id = setup_offer(admin)
    conns = [psycopg.connect(DSN, autocommit=True) for _ in range(BUYERS)]
    report = {
        "escenario": {"compradores": BUYERS, "porciones": PORTIONS, "repeticiones": REPS},
        "postgres": admin.execute("show server_version").fetchone()[0],
        "mecanismos": {},
    }
    print(f"{BUYERS} compradores x {PORTIONS} porciones x {REPS} repeticiones\n")
    for name, mechanism in MECHANISMS.items():
        oversold_rounds, max_sold, all_lat = 0, 0, []
        for _ in range(REPS):
            reset_offer(admin, offer_id)
            sold, lat = run_round(conns, mechanism, offer_id)
            final = admin.execute(
                "select available_portions from offer where id = %s", (offer_id,)
            ).fetchone()[0]
            if sold > PORTIONS or sold + final != PORTIONS:
                oversold_rounds += 1
            max_sold = max(max_sold, sold)
            all_lat.extend(lat)
        res = {
            "repeticiones_con_sobreventa": oversold_rounds,
            "max_porciones_vendidas": max_sold,
            "latencia_ms_p50": round(statistics.median(all_lat), 1),
            "latencia_ms_p95": round(percentile(all_lat, 95), 1),
            "latencia_ms_max": round(max(all_lat), 1),
        }
        report["mecanismos"][name] = res
        print(
            f"{name:28} sobreventas en {oversold_rounds:3}/{REPS} rep. | máx vendidas {max_sold:2}"
            f" | p50 {res['latencia_ms_p50']} ms | p95 {res['latencia_ms_p95']} ms"
        )
    out = Path(__file__).with_name("resultados_stock.json")
    out.write_text(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"\nGuardado en {out}")
    with admin.transaction():
        admin.execute("delete from offer where id = %s", (offer_id,))
        admin.execute("delete from pickup_point where id = %s", (point_id,))
        admin.execute("delete from app_user where id = %s", (seller_id,))


if __name__ == "__main__":
    main()

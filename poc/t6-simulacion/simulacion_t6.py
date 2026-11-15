import json
import math
import random

CELL_SIZE_M = 400.0
METERS_PER_DEG_LAT = 111_320.0


def meters_per_deg_lon(lat: float) -> float:
    return METERS_PER_DEG_LAT * math.cos(math.radians(lat))


def haversine(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6_371_000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.atan2(math.sqrt(a), math.sqrt(1 - a))


def cell_centroid(lat: float, lon: float) -> tuple[float, float]:
    m_lon = meters_per_deg_lon(lat)
    cy = (math.floor((lat * METERS_PER_DEG_LAT) / CELL_SIZE_M) + 0.5) * CELL_SIZE_M
    cx = (math.floor((lon * m_lon) / CELL_SIZE_M) + 0.5) * CELL_SIZE_M
    return round(cy / METERS_PER_DEG_LAT, 6), round(cx / m_lon, 6)


def simulate_trilateration(offer: dict, n_queries: int = 50) -> dict:
    real_lat, real_lon = offer["lat"], offer["lon"]
    c_lat, c_lon = cell_centroid(real_lat, real_lon)
    random.seed(offer["id"])

    queries = []
    for i in range(1, n_queries + 1):
        angle = (2 * math.pi * i) / n_queries
        radius = random.uniform(300.0, 1200.0)
        q_lat = c_lat + (radius * math.sin(angle)) / METERS_PER_DEG_LAT
        q_lon = c_lon + (radius * math.cos(angle)) / meters_per_deg_lon(c_lat)

        dist_100m = round(haversine(q_lat, q_lon, c_lat, c_lon) / 100.0) * 100
        queries.append({
            "query": i,
            "observer": [round(q_lat, 6), round(q_lon, 6)],
            "reported_centroid": [c_lat, c_lon],
            "reported_distance_m": dist_100m,
        })

    # Estimación por trilateración (promedio de posiciones reportadas)
    est_lat = sum(q["reported_centroid"][0] for q in queries) / n_queries
    est_lon = sum(q["reported_centroid"][1] for q in queries) / n_queries

    return {
        "offer_id": offer["id"],
        "real_coords": [real_lat, real_lon],
        "cell_centroid": [c_lat, c_lon],
        "actual_error_m": round(haversine(real_lat, real_lon, c_lat, c_lon), 2),
        "attacker_residual_error_m": round(haversine(real_lat, real_lon, est_lat, est_lon), 2),
        "queries": queries,
    }


def main():
    max_error_m = round(math.hypot(CELL_SIZE_M / 2, CELL_SIZE_M / 2), 2)
    offers = [
        {"id": 1, "lat": -36.827012, "lon": -73.050318},
        {"id": 2, "lat": -36.831450, "lon": -73.035890},
        {"id": 3, "lat": -36.819820, "lon": -73.061240},
    ]

    output = {
        "cell_size_m": CELL_SIZE_M,
        "max_theoretical_error_m": max_error_m,
        "simulations": [simulate_trilateration(o, 50) for o in offers],
    }

    with open("poc/t6-simulacion/resultados_t6.json", "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2)

    print(f"T6 OK | Error max teorico: {max_error_m}m | Guardado en poc/t6-simulacion/resultados_t6.json")


if __name__ == "__main__":
    main()
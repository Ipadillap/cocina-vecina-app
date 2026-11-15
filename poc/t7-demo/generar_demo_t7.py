import json
import math
from datetime import datetime, timedelta, timezone

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


def main():
    buyer_lat, buyer_lon = -36.828, -73.038  # UdeC redondeado a 100m
    now = datetime.now(timezone.utc)
    prepared_at = (now - timedelta(minutes=30)).strftime("%Y-%m-%dT%H:%M:%SZ")

    seed_offers = [
        (101, 11, "Sra. Marta", "Empanadas de Pino (2 un.)", "EMPANADAS", 3500, 3000, 6, ["GLUTEN", "EGG"], 3, -36.827650, -73.038920),
        (102, 12, "Cocina Don Carlos", "Lasaña Boloñesa Casera", "MAIN_COURSE", 4500, 3800, 4, ["GLUTEN", "MILK", "EGG"], 4, -36.826410, -73.042150),
        (103, 13, "Panadería La Esquina", "Pan Amasado Integral (4 un.)", "HOMEMADE_BREAD", 1800, 1500, 8, ["GLUTEN", "SESAME"], 5, -36.822980, -73.044800),
        (104, 14, "Delicias del Sur", "Cazuela de Vacuno", "SOUPS_AND_CREAMS", 4200, 3500, 3, ["NONE_KNOWN"], 2, -36.826950, -73.050120),
        (105, 15, "Dulce Conce", "Kuchen de Nuez y Manzana", "PASTRY", 2800, 2200, 5, ["GLUTEN", "MILK", "EGG", "TREE_NUTS"], 6, -36.832100, -73.046700),
        (106, 16, "Verde Vivo", "Bowl Quínoa y Palta", "SALADS", 3900, 3200, 4, ["SESAME"], 3, -36.838500, -73.052300),
        (107, 17, "Sabores de Casa", "Porotos con Riendas", "MAIN_COURSE", 3800, 3000, 5, ["GLUTEN"], 3, -36.821400, -73.018900),
        (108, 18, "Sra. Elena", "Chaparritas Queso Jamón (3 un.)", "EMPANADAS", 2500, 2000, 7, ["GLUTEN", "MILK"], 4, -36.825100, -73.048900),
    ]

    nearby, details = [], []
    for oid, sid, seller, title, cat, base_p, min_p, portions, allergens, hrs, r_lat, r_lon in seed_offers:
        a_lat, a_lon = cell_centroid(r_lat, r_lon)
        dist_100m = round(haversine(buyer_lat, buyer_lon, a_lat, a_lon) / 100.0) * 100
        expires_at = (now + timedelta(hours=hrs)).strftime("%Y-%m-%dT%H:%M:%SZ")
        photo = f"/photos/offer_{oid}.webp"

        nearby.append({
            "id": oid, "title": title, "category": cat, "photoUrl": photo,
            "basePrice": base_p, "minPrice": min_p, "availablePortions": portions,
            "expiresAt": expires_at, "approxLatitude": a_lat, "approxLongitude": a_lon,
            "distanceMeters": dist_100m,
        })
        details.append({
            "id": oid, "sellerId": sid, "seller": seller, "title": title,
            "description": f"{title} - Preparación casera del día.", "category": cat,
            "photoUrl": photo, "basePrice": base_p, "minPrice": min_p,
            "availablePortions": portions, "allergens": allergens,
            "preparedAt": prepared_at, "expiresAt": expires_at, "status": "ACTIVE",
            "approxLatitude": a_lat, "approxLongitude": a_lon,
        })

    nearby.sort(key=lambda x: x["distanceMeters"])

    output = {
        "buyer_reference": {"approxLatitude": buyer_lat, "approxLongitude": buyer_lon},
        "nearby_offers": nearby,
        "offer_details": details,
    }

    with open("poc/t7-demo/demo_data.json", "w", encoding="utf-8") as f:
        json.dump(output, f, indent=2, ensure_ascii=False)

    print(f"T7 OK | {len(nearby)} ofertas generadas en poc/t7-demo/demo_data.json")


if __name__ == "__main__":
    main()
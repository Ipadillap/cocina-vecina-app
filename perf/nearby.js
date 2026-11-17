// RNF-R-1 y RNF-R-3: búsqueda cercana con carga sostenida.
// Mezcla 70/20/10: 70 % búsqueda cercana, 20 % detalle de oferta, 10 % ofertas propias.
// 10 req/s durante 10 min. Requiere ofertas cargadas (por ejemplo db/seed-demo.sql).
//
// Uso:
//   k6 run -e BASE_URL=https://api.ejemplo.cl -e EMAIL=demo@... -e PASSWORD=... \
//     --summary-export perf/resultados_nearby.json perf/nearby.js
// Leer http_req_duration{name:nearby} p(95) y http_req_failed en el resumen.
import http from "k6/http";
import { check } from "k6";

const BASE = __ENV.BASE_URL || "http://localhost:8088";

export const options = {
  scenarios: {
    carga: {
      executor: "constant-arrival-rate",
      rate: 10,
      timeUnit: "1s",
      duration: __ENV.DURATION || "10m",
      preAllocatedVUs: 20,
      maxVUs: 50,
    },
  },
  thresholds: {
    "http_req_duration{name:nearby}": ["p(95)<300"],
    http_req_failed: ["rate<0.01"],
  },
};

// Puntos de búsqueda en el Gran Concepción
const POINTS = [
  [-36.8270, -73.0503], // Concepción centro
  [-36.8200, -73.0400], // Barrio Universitario
  [-36.7240, -73.1170], // Talcahuano
  [-36.8430, -73.1080], // San Pedro de la Paz
  [-36.7800, -73.0820], // Hualpén
];

export function setup() {
  const res = http.post(
    `${BASE}/v1/auth/login`,
    JSON.stringify({ email: __ENV.EMAIL, password: __ENV.PASSWORD }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(res, { "login 200": (r) => r.status === 200 });
  const token = res.json("accessToken") || res.json("data.accessToken");
  const auth = { headers: { Authorization: `Bearer ${token}` } };
  const [lat, lon] = POINTS[0];
  const list = http.get(`${BASE}/v1/offers/nearby?lat=${lat}&lon=${lon}&radius=5000`, auth);
  const body = list.json();
  const items = Array.isArray(body) ? body : body.data || [];
  return { auth, ids: items.map((o) => o.id).filter(Boolean) };
}

export default function (data) {
  const r = Math.random();
  if (r < 0.7 || data.ids.length === 0) {
    const [lat, lon] = POINTS[Math.floor(Math.random() * POINTS.length)];
    const radius = [500, 1500, 3000, 5000][Math.floor(Math.random() * 4)];
    const res = http.get(`${BASE}/v1/offers/nearby?lat=${lat}&lon=${lon}&radius=${radius}`,
      { ...data.auth, tags: { name: "nearby" } });
    check(res, { "nearby 200": (x) => x.status === 200 });
  } else if (r < 0.9) {
    const id = data.ids[Math.floor(Math.random() * data.ids.length)];
    const res = http.get(`${BASE}/v1/offers/${id}`, { ...data.auth, tags: { name: "detail" } });
    check(res, { "detail 200": (x) => x.status === 200 });
  } else {
    const res = http.get(`${BASE}/v1/offers/mine`, { ...data.auth, tags: { name: "mine" } });
    check(res, { "mine 200": (x) => x.status === 200 });
  }
}

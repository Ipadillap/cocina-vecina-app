# ADR 0001 · Monolito modular con PostgreSQL + PostGIS

**Estado:** aceptada (E2).

**Contexto.** Equipo de seis personas a 10 h/semana, operación ≤ CLP $20.000/mes en un servidor, y dos
requisitos que dominan el diseño: cero sobreventas con aceptaciones concurrentes (RNF-R-4) y búsqueda por
radio con privacidad del domicilio (RF-4, RB-6).

**Decisión.** Un solo proceso Spring Boot dividido en módulos por dominio, con una base PostgreSQL + PostGIS.
El stock se reserva con una actualización atómica condicionada; la búsqueda usa `ST_DWithin` sobre
`geography` con índice GiST. Las tareas programadas corren en un segundo proceso de la misma imagen.

**Consecuencias.** Consistencia transaccional sin coordinación distribuida y un solo artefacto que desplegar.
A cambio, un único nodo (disponibilidad acotada a ~99 %, RNF-D-1). Los límites entre módulos permiten
separar un módulo en un servicio si algún día lo justificara la carga.

**Alternativas descartadas.** Servicios separados con Redis como índice y bus de eventos (consistencia
eventual en el stock y más operación) y microservicios con API gateway (sobreingeniería para el calendario).

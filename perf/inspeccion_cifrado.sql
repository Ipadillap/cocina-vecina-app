-- RNF-S-3: inspección de la base de demo. Ningún dato sensible debe quedar legible.
-- Uso: psql "$DATABASE_URL" -f perf/inspeccion_cifrado.sql   (guardar captura del resultado)
--
-- Un valor cifrado por FieldEncryptor es Base64 de (IV 12 B + texto cifrado + etiqueta 16 B),
-- así que nunca es una dirección, una coordenada decimal ni un teléfono legible.

\echo '== Puntos de retiro: filas y valores con apariencia legible =='
select count(*)                                                          as filas,
       count(*) filter (where encrypted_address ~ '[[:space:]]')          as direcciones_con_espacios,
       count(*) filter (where encrypted_lat ~ '^-?[0-9]+\.[0-9]+$')        as latitudes_en_claro,
       count(*) filter (where encrypted_lon ~ '^-?[0-9]+\.[0-9]+$')        as longitudes_en_claro,
       count(*) filter (where encrypted_address !~ '^[A-Za-z0-9+/]+=*$')  as direcciones_no_base64
from pickup_point;

\echo '== Usuarios: teléfonos en claro =='
select count(*) filter (where encrypted_phone is not null)               as con_telefono,
       count(*) filter (where encrypted_phone ~ '^\+?[0-9 ]{8,}$')         as telefonos_en_claro
from app_user;

\echo '== Muestra (deben verse como texto Base64 ilegible) =='
select id, left(encrypted_address, 40) as direccion, left(encrypted_lat, 24) as lat
from pickup_point order by id limit 3;

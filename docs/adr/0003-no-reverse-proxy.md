# ADR 0003 · Sin proxy inverso: TLS en la API

**Estado:** aceptada (reemplaza a Caddy como punto de entrada).

**Contexto.** El despliegue tenía Caddy delante de la API y de Nginx para obtener el certificado, agregar
HSTS y enrutar `/v1/*` y `/fotos/*` por un solo dominio. Es un contenedor más que operar en un servidor de
2 GB, y sus tareas ya las puede hacer el propio stack: Spring Boot termina TLS con un *SSL bundle*, Spring
Security agrega HSTS y la API ya limita las búsquedas por usuario (RNF-S-5).

**Decisión.** La API publica el puerto 443 con el perfil `tls` (certificado PEM de Let's Encrypt obtenido con
`certbot --standalone` y recargado en caliente con `reload-on-update`). Nginx sirve las fotos por HTTPS en el
puerto 8443 con el mismo certificado. La app usa la URL absoluta de las fotos que entrega la API.

**Consecuencias.** Un contenedor menos y una configuración menos que mantener. A cambio, la API y Nginx
publican puertos distintos, la renovación del certificado depende del *deploy hook*
(`infra/install-certificate.sh`) y el puerto 80 debe quedar libre para certbot. Si las fotos pasan a
Cloudflare R2, Nginx deja de ser necesario y queda un solo puerto público.

#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
source_dir="${RENEWED_LINEAGE:-/etc/letsencrypt/live/${1:?usage: install-certificate.sh <domain>}}"
install -d -m 755 tls
install -m 644 "$source_dir/fullchain.pem" tls/fullchain.pem
install -m 600 -o 10001 "$source_dir/privkey.pem" tls/privkey.pem
if [ -n "$(docker compose ps -q photos 2>/dev/null)" ]; then
    docker compose exec -T photos nginx -s reload
fi
echo "Certificate installed in $(pwd)/tls"

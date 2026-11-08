#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p backups
file="backups/cocinavecina-$(date +%F).sql.gz"
docker compose exec -T db pg_dump -U cocinavecina cocinavecina | gzip > "$file"
find backups -name "cocinavecina-*.sql.gz" -mtime +7 -delete
echo "Backup ready: $file"

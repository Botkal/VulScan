#!/usr/bin/env bash
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080/api/inventory/import}"
OUT="${OUT:-/tmp/SoftwareInventory.csv}"

HOSTNAME="$(hostname)"
ASSET_TAG="${ASSET_TAG:-}"
SOURCE="linux_script"

echo "source,hostname,asset_tag,vendor,product,version,cpe,installed_on" > "$OUT"

# Debian/Ubuntu
if command -v dpkg-query >/dev/null 2>&1; then
  dpkg-query -W -f='${Package},${Version}\n' | while IFS=',' read -r pkg ver; do
    # vendor/cpe/installed_on üresen marad (demóra oké)
    echo "${SOURCE},\"${HOSTNAME}\",\"${ASSET_TAG}\",\"\",\"${pkg}\",\"${ver}\",\"\",\"\"" >> "$OUT"
  done
# RHEL/Fedora/SUSE
elif command -v rpm >/dev/null 2>&1; then
  rpm -qa --qf '%{NAME},%{VERSION}-%{RELEASE}\n' | while IFS=',' read -r pkg ver; do
    echo "${SOURCE},\"${HOSTNAME}\",\"${ASSET_TAG}\",\"\",\"${pkg}\",\"${ver}\",\"\",\"\"" >> "$OUT"
  done
else
  echo "Nem találtam dpkg-query-t vagy rpm-et. Csak Debian/Ubuntu vagy RPM alapú rendszerek támogatottak." >&2
  exit 1
fi

echo "CSV kész: $OUT"
echo "Feltöltés: $API_URL"

curl -sS -F "file=@${OUT}" "$API_URL"
echo

#!/bin/sh
set -eu
mode=${1:---dry-run}
case "$mode" in
 --dry-run) ;;
 --apply) echo 'Apply blocked: physical preflight and approved interface/firewall plan required.' >&2; exit 2 ;;
 *) echo 'Usage: wg-setup.sh [--dry-run|--apply]' >&2; exit 2 ;;
esac
echo 'Aurora WireGuard setup plan (read-only)'
for c in wg ip wireguard-go qrencode; do
 if command -v "$c" >/dev/null 2>&1; then echo "$c: available"; else echo "$c: unavailable"; fi
done
[ ! -c /dev/net/tun ] || echo 'TUN device present (functional test pending)'
grep -i wireguard /proc/modules 2>/dev/null || :
echo 'Planned: dedicated interface, nonoverlapping subnet, own peer keys, scoped rules, backup, watchdog rollback.'
echo 'No writes, package installs, firewall changes, D2K/DoQ changes or key generation performed.'
echo 'Apply, rollback and QR generation not implemented until hardware plan validated.'

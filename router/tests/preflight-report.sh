#!/bin/sh
set -eu
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir "$tmp/bin"

cat > "$tmp/bin/ip" <<'MOCK'
#!/bin/sh
case "$*" in
 '-4 route show default') echo 'default via 100.64.1.1 dev private-wan' ;;
 '-4 addr show dev private-wan') printf '%s\n' ' inet 100.64.1.2/24' ' inet 192.168.52.1/24' ' inet 203.0.113.9/24' ' inet 9.8.7.6/24' ;;
 '-6 route show default') echo 'default via fe80::abcd dev private-wan' ;;
 *) exit 1 ;;
esac
MOCK
cat > "$tmp/bin/ndmc" <<'MOCK'
#!/bin/sh
case "$2" in
 'show version') printf '%s\n' 'release: 4.3.1' 'title: private-router' ;;
 'show components') printf '%s\n' 'id: private-component' 'id: wireguard' ;;
 *) exit 1 ;;
esac
MOCK
cat > "$tmp/bin/df" <<'MOCK'
#!/bin/sh
printf '%s\n' 'Filesystem 1024-blocks Used Available Capacity Mounted on' '//private-host/private-share 1024 256 768 25% /opt'
MOCK
chmod +x "$tmp/bin/"*

PATH="$tmp/bin:$PATH" busybox sh router/preflight.sh > "$tmp/report"

for line in 'WAN: CGNAT_RANGE' 'WAN: PRIVATE_RANGE' 'WAN: SPECIAL_RANGE' 'WAN: OTHER_RANGE' \
 'IPv6 default route: PRESENT' 'release: 4.3.1' 'Keenetic WireGuard component: listed' 'opt available KiB: 768'; do
 grep -Fqx "$line" "$tmp/report"
done
if grep -Eq 'private-|100[.]64[.]1[.]|192[.]168[.]52[.]|203[.]0[.]113[.]|9[.]8[.]7[.]6|fe80::abcd' "$tmp/report"; then
 echo 'Preflight report exposed a fixture identifier' >&2
 exit 1
fi

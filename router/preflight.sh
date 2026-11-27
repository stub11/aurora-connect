#!/bin/sh
# Read-only; no files created, no modules loaded.
echo '=== Aurora preflight ==='
uname -m
uname -r
[ -c /dev/net/tun ] && echo TUN_PRESENT || echo TUN_NOT_FOUND
command -v wg || :
grep -i wireguard /proc/modules 2>/dev/null || :
if [ -r /proc/config.gz ]; then
 zcat /proc/config.gz | grep -E '^CONFIG_(WIREGUARD|TUN)=' || :
fi
for d in /lib/modules /opt/lib/modules; do
 [ ! -d "$d" ] || find "$d" -name '*wireguard*' 2>/dev/null
done
echo '=== WAN ==='
ip -4 route show default 2>/dev/null | awk '{for(i=1;i<=NF;i++) if($i=="dev") print "interface=" $(i+1)}'
echo 'CGNAT: compare WAN and external IP locally; do not publish addresses'
echo '=== DNS ==='
netstat -ln 2>/dev/null | awk '$4 ~ /:(53|5354)$/ {print $1, "DNS listener"}'
echo '=== Firewall ==='
for c in iptables ip6tables nft; do command -v "$c" || :; done
echo '=== RAM / storage ==='
grep -E '^(MemTotal|MemAvailable|MemFree|SwapTotal|SwapFree):' /proc/meminfo
df -k /opt
awk '$2=="/opt" {print "opt mounted:",$3}' /proc/mounts
if command -v opkg >/dev/null 2>&1; then
 opkg --version
 opkg print-architecture
fi
echo '=== Services ==='
for c in doqd d2k; do if pidof "$c" >/dev/null 2>&1; then echo "$c running"; else echo "$c process not detected"; fi; done
echo 'Functional tests: НЕ ПРОВЕРЕНО'

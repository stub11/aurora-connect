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
# Inspect all default-route interfaces; never print address or gateway.
wan_devs=$(ip -4 route show default 2>/dev/null | awk '{for(i=1;i<NF;i++) if($i=="dev") print $(i+1)}' | sort -u)
if [ -z "$wan_devs" ]; then echo 'WAN: UNKNOWN'; fi
for dev in $wan_devs; do
 ip -4 addr show dev "$dev" 2>/dev/null | awk '
 $1=="inet" {
  split($2,p,"/"); split(p[1],a,"."); n++
  if(a[1]==100 && a[2]>=64 && a[2]<=127) print "WAN: CGNAT_RANGE"
  else if(a[1]==10 || (a[1]==172 && a[2]>=16 && a[2]<=31) || (a[1]==192 && a[2]==168)) print "WAN: PRIVATE_RANGE"
  else if(a[1]==0 || a[1]==127 || (a[1]==169 && a[2]==254) || a[1]>=224 || (a[1]==192 && a[2]==0) || (a[1]==198 && (a[2]==18 || a[2]==19)) || (a[1]==198 && a[2]==51 && a[3]==100) || (a[1]==203 && a[2]==0 && a[3]==113)) print "WAN: SPECIAL_RANGE"
  else print "WAN: OTHER_RANGE"
 }
 END {if(!n) print "WAN: UNKNOWN"}'
done
echo 'CGNAT classification is indicative; compare external IP locally.'
echo '=== IPv6 ==='
v6=$(ip -6 route show default 2>/dev/null) && {
 if [ -n "$v6" ]; then echo 'IPv6 default route: PRESENT'; else echo 'IPv6 default route: ABSENT'; fi
} || echo 'IPv6 default route: UNKNOWN'
echo '=== Keenetic ==='
if command -v ndmc >/dev/null 2>&1; then
 ndmc -c 'show version' 2>/dev/null | awk '/^[[:space:]]*(release|version|ndm|title|arch):[[:space:]]*/ {if ($2 ~ /^[A-Za-z0-9._-]+$/ && NF==2) print $1,$2}'
 ndmc -c 'show components' 2>/dev/null | awk '/^[[:space:]]*(name|id|version|installed):[[:space:]]*/ {if ($2 ~ /^[A-Za-z0-9._-]+$/ && NF==2) print $1,$2}'
 echo 'Keenetic component output may be incomplete on this firmware.'
else echo 'ndmc: unavailable'; fi
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

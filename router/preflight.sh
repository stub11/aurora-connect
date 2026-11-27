#!/bin/sh
# Read-only; no network requests, no addresses or credentials in output.
set -u
export LC_ALL=C
has() { command -v "$1" >/dev/null 2>&1; }
flag() { if "$@"; then echo yes; else echo no; fi; }
echo 'Aurora preflight v0.1'
printf 'ABI='; uname -m
printf 'kernel='; uname -r
printf 'tun_node='; flag test -c /dev/net/tun
printf 'wg_tool='; flag has wg
printf 'wg_userspace='; flag has wireguard-go
printf 'wg_loaded='; flag grep -q '^wireguard ' /proc/modules
if [ -r /proc/config.gz ] && has zcat; then
  zcat /proc/config.gz | grep -E '^CONFIG_(WIREGUARD|TUN)=' || :
else
  echo 'kernel_config=unknown'
fi
if has ip; then
  ip -4 route show default 2>/dev/null | awk 'END {print "default_ipv4_routes=" NR}'
  ip -6 route show default 2>/dev/null | awk 'END {print "default_ipv6_routes=" NR}'
else
  echo 'routing=unknown'
fi
echo 'CGNAT=unknown (requires private WAN/external IP comparison)'
for tool in iptables ip6tables nft opkg; do
  printf '%s=' "$tool"; flag has "$tool"
done
awk '/^MemTotal:|^MemAvailable:|^SwapTotal:/ {print}' /proc/meminfo
if [ -d /opt ]; then
  df -Pk /opt | awk 'NR==2 {print "opt_available_kib=" $4}'
else
  echo 'opt=absent'
fi
if has opkg; then opkg print-architecture 2>/dev/null || :; fi
for svc in doqd d2k; do
  printf '%s_process=' "$svc"; if pidof "$svc" >/dev/null 2>&1; then echo yes; else echo no; fi
done
# No resolver addresses or full firewall rules are exported.
printf 'resolver_file='; flag test -r /etc/resolv.conf
echo 'END'

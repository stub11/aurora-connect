# Hardware batch (pending)
On Windows PowerShell, download reviewed router/preflight.sh from the merged repository to D:\AI\preflight.sh, then run:
```powershell
Get-Content -Raw D:\AI\preflight.sh | ssh -p 222 root@<router-LAN-address> sh | Tee-Object -FilePath D:\AI\aurora-preflight.txt
```
Replace placeholder locally. Post sanitized report in hardware issue; no IPs, keys or credentials. Compare WAN IP with external IP locally and report equal/different and private/CGNAT/public classification. On phone disable Wi-Fi, test existing authorized endpoints with ready clients, record operator, restrictions, transport, actual traffic and DNS/IPv6 results. No configured endpoint = НЕ ПРОВЕРЕНО. PASS preflight means report collected, not VPN functional. No router configuration changes.

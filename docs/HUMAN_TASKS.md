# Hardware batch (pending)
On Windows PowerShell, download reviewed router/preflight.sh from the merged repository to D:\AI\preflight.sh, then run:
```powershell
Get-Content -Raw D:\AI\preflight.sh | ssh -p 222 root@<router-LAN-address> sh | Tee-Object -FilePath D:\AI\aurora-preflight.txt
```
Replace placeholder locally. Post sanitized report in hardware issue; no IPs, keys or credentials. Compare WAN IP with external IP locally and report equal/different and private/CGNAT/public classification. On phone disable Wi-Fi, test existing authorized endpoints with ready clients, record operator, restrictions, transport, actual traffic and DNS/IPv6 results. No configured endpoint = НЕ ПРОВЕРЕНО. PASS preflight means report collected, not VPN functional. No router configuration changes.

## WireGuard setup planner
After PR merge, review router/wg-setup.sh and run it via SSH stdin with sh -s -- --dry-run. Return tool availability and TUN presence only, alongside existing issue #5. Do not run --apply: deliberately blocked pending hardware evidence. Kernel WG preferred if available. Userspace wireguard-go requires compatible ARM64 binary and working TUN; CPU/RAM overhead must be measured. Native Keenetic WireGuard may offer supported UI and persistent routing, but ported firmware component availability and inbound peer configuration must be checked. No D2K/DoQ changes.

## Защита main (GitHub в мобильном браузере)
1. Открой https://github.com/stub11/aurora-connect/settings и включи версию сайта для ПК при необходимости.
2. Settings → Branches → Add branch protection rule (или Rules → Rulesets → New branch ruleset).
3. Выбери main; для ruleset установи Active.
4. Включи Require a pull request before merging, без обхода для администраторов.
5. Включи Require status checks to pass: выбери фактические проверки android, syntax, validate соответствующих workflows (Android CI, Router preflight CI, Legacy runner check). Сверь имена в Checks.
6. Запрети force push и удаление main, включи Do not allow bypassing; сохрани правило.
7. Проверь на следующем PR, что merge блокируется до успешного CI. Не включай Restrict updates без проверки возможности merge.

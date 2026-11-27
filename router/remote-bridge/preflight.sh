#!/bin/sh
# No token, no message, no route changes. TLS verification stays enabled.
if command -v curl >/dev/null 2>&1 && curl -q --noproxy '*' --proto '=https' --connect-timeout 5 --max-time 12 -s -o /dev/null -w '%{http_code}' https://api.telegram.org/ 2>/dev/null | grep -Eq '^[234][0-9][0-9]$'; then
 echo 'TELEGRAM_FROM_ROUTER: OK'
else
 echo 'TELEGRAM_FROM_ROUTER: FAIL'
 exit 1
fi

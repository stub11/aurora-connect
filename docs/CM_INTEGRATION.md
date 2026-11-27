# CM Aurora integration

Approved direction: one end-user Android application, independent public networking module. Keep standalone test harness for development only.

## Boundary
Expose a versioned Android library API for connect, disconnect, state observation and capability checks. CM owns navigation and presentation; Connect owns VPN lifecycle, routing and key handling. No dependency on private CM source. No credentials in public artifacts. Only one active Android VPN service per user profile; report conflicts without silently disconnecting other apps.

## Lifecycle
CM monitoring must remain functional with the module absent, disabled or disconnected. Use explicit user consent via VpnService.prepare. Keep VPN foreground lifecycle independent of activity lifecycle. Do not start a tunnel automatically on app launch. Store keys through Android Keystore-backed protection; do not export private keys to CM logs or telemetry.

## Integration gates
1. Define API and fake implementation before UI wiring.
2. Test library independently with unit tests and Android emulator.
3. Integrate behind disabled-by-default feature flag in private CM branch, preserving approved screens.
4. Verify no regressions in SSH, monitoring, D2K/DoQ, app upgrades, VPN consent and disconnect.
5. Enable only after physical router and mobile-network acceptance. Restricted network availability remains unverified until measured.

## Distribution
Public library and test harness may be released independently. CM consumes a pinned, checksum-verified version. Final user experience is a single CM APK; standalone harness is not a prerequisite for users.

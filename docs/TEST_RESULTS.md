# Evidence

- Android 16 on a physical phone. dev-34 started and checked for updates. dev-40 was installed and started, reported no newer build, and the tester confirmed the download progress bar was visible. [dev-40](https://github.com/stub11/aurora-connect/releases/tag/dev-40)
- Android 16, mobile 4G with restricted access. Aurora Connect started a Full tunnel with a Proton Free test profile. The transmit counter increased and receive remained zero over the observed interval. WireGuard handshake and internet reachability were not confirmed. No Wi-Fi control was available.
- GitHub runner tooling passed. [CI run](https://github.com/stub11/aurora-connect/actions/runs/45964505646)

Live VPN traffic, DNS and IPv6, reconnect and router acceptance remain unverified.

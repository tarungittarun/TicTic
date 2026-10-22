# MySpace debug APKs

Each build has a SHA-256 sidecar. These are debug builds signed with the CI debug key, not
store-signed releases: installable directly, and they will not overwrite or be overwritten by a
Play install because the applicationId changed from the TicTic builds above to `app.space.one`.

The TicTic rows are the last builds of the single clock app. The MySpace rows are builds of the
suite. From v3.0.0 the APK is committed by CI on every push that changes the version or
versionCode, so a testable build is always downloadable from this directory rather than only
from an expiring Actions artifact.

| Version | APK | Size | SHA-256 |
|---|---|---:|---|
| 1.9.9 | [Download APK](TicTic-v1.9.9-debug.apk) | 699,616 bytes | `f84543d7583914a8b0ab482a7ab33c4797b44aa9f3d72150009006fb68427da1` |
| 1.10.0 | [Download APK](TicTic-v1.10.0-debug.apk) | 703,423 bytes | `2bc03738ade4b7103c8a13b5b75b1d6478edfd332fc2998e962d7896d1ff33cb` |
| 1.11.0 | [Download APK](TicTic-v1.11.0-debug.apk) | 714,290 bytes | `b7676d525f3308b9c800ed49e3ef2359ef2085d53a8b06b70d5705e068c00935` |
| 1.11.1 | [Download APK](TicTic-v1.11.1-debug.apk) | 714,405 bytes | `e1023e41c216b9f34bc419164022af4e774118a9e99762a51f7300628f3bd5b1` |
| 1.11.2 | [Download APK](TicTic-v1.11.2-debug.apk) | 724,118 bytes | `746e979746481d3bd1c6f4849aef0afaf3161948d698464c77a259d95762faa2` |
| 3.0.0 (build 24) | [Download APK](MySpace-v3.0.0-24-debug.apk) | 878434 bytes | `a628e30c488a04db8d1c63db48a167d9757829dc1decb713b04cc41039fae27c` |

Retained versions: 1.9.9, 1.10.0, 1.11.0, 1.11.1 and 1.11.2. The v1.11.2 build contains the responsive Settings/Alarm Defaults/Widget Studio update, five glass presets and five selectable touch-feedback styles. Runtime/device QA for v1.11.2 remains NOT RUN.

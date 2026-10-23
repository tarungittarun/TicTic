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
| 3.1.0 (build 25) | [Download APK](MySpace-v3.1.0-25-debug.apk) | 879694 bytes | `241ed87dc5833b066e572e5dd25fc67f8b8b2aa52d2924c43842f2bf94b356c1` |
| 3.2.0 (build 26) | [Download APK](MySpace-v3.2.0-26-debug.apk) | 882954 bytes | `e6c889dab302be9b7305e1bce63b110ffa1c7b810e0f7eafec441ccf3fdae27a` |
| 3.3.0 (build 27) | [Download APK](MySpace-v3.3.0-27-debug.apk) | 885150 bytes | `94cbd0255068d06b5ae80fc9e6d216c976640152c30f019ba948bae03d320e78` |
| 3.4.0 (build 28) | [Download APK](MySpace-v3.4.0-28-lite-debug.apk) | 885618 bytes | `d7e871bd146e394e3f0649c251cfbb0b49fb5b75713f87985a295e15601620c3` |
| 3.5.0 (build 29) | [Download APK](MySpace-v3.5.0-29-lite-dist.apk) | 749807 bytes | `7c39c5392e43103b8e67ce1904e9c43c06a1d349ea63409cfe03d0c1324d147e` |
| 3.6.0 (build 30) | [Download APK](MySpace-v3.6.0-30-lite-dist.apk) | 754155 bytes | `33dcdbd08d9dbe9690e54aefd1cb5de9e3593fed5369930c0136abb63baae3aa` |
| 3.7.0 (build 31) | [Download APK](MySpace-v3.7.0-31-lite-dist.apk) | 755515 bytes | `397393769b98da2dfffd39ea3d84cd678fd89eb765116ce6cca1cfb3748993b3` |
| 3.8.0 (build 32) | [Download APK](MySpace-v3.8.0-32-lite-dist.apk) | 767951 bytes | `2acefffc3e3770dc7e28ec4f7cd340453d81c28791686c6f0fb93c1d3586dab1` |
| 3.9.0 (build 33) | [Download APK](MySpace-v3.9.0-33-lite-dist.apk) | 776483 bytes | `44b9536f85706c079a19784903204863610fff4976827988c1b337b9cd200fc8` |
| 3.10.0 (build 34) | [Download APK](MySpace-v3.10.0-34-lite-dist.apk) | 791143 bytes | `26791ec883189a042d70d020e03a71dbd978956d3a29f00c1d218f24b57fbf2d` |
| 3.11.0 (build 35) | [Download APK](MySpace-v3.11.0-35-lite-dist.apk) | 2059932 bytes | `5e23c53c4fafb39ee295089f4e8ca7bbdef7d70ecfc5775659bdd859dda5f675` |
| 3.11.1 (build 36) | [Download APK](MySpace-v3.11.1-36-lite-dist.apk) | 2060312 bytes | `daed2bd3d8b6d5fd3f3670741b3e49ba41ebc5633d8d76149ec72984ec0de46d` |
| 3.12.0 (build 37) | [Download APK](MySpace-v3.12.0-37-lite-dist.apk) | 2064668 bytes | `745675832483c826b7dfa3e6f0cfb807722405b2db0a3524d1cbd0a41b645dc5` |
| 3.12.1 (build 38) | [Download APK](MySpace-v3.12.1-38-lite-dist.apk) | 2064524 bytes | `fce51890f4ba5fe8aa5a5dbcbd12ace7c7e9c152beb1b70dfb979ef0257538d8` |

Retained versions: 1.9.9, 1.10.0, 1.11.0, 1.11.1 and 1.11.2. The v1.11.2 build contains the responsive Settings/Alarm Defaults/Widget Studio update, five glass presets and five selectable touch-feedback styles. Runtime/device QA for v1.11.2 remains NOT RUN.

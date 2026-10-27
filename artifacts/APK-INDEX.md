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
| 3.12.2 (build 39) | [Download APK](MySpace-v3.12.2-39-lite-dist.apk) | 2064840 bytes | `9547856a720d8fd968e80e184a624c68ad8077332597b67b43fb7b4983a5c236` |
| 3.12.3 (build 40) | [Download APK](MySpace-v3.12.3-40-lite-dist.apk) | 2070464 bytes | `b3048cbc7dcac5c17c56f305c1a63729ccf5ea533620cd1eab87a318f861688f` |
| 3.12.4 (build 41) | [Download APK](MySpace-v3.12.4-41-lite-dist.apk) | 2073512 bytes | `ba8e60cf0fdccf6b62122e2a1c491f51f93bf7891e749bbcffd28a2f6dca1479` |
| 3.12.5 (build 42) | [Download APK](MySpace-v3.12.5-42-lite-dist.apk) | 2074712 bytes | `7bb5ebeb41bfb82553ffe27a055d7d08a7720ad0d2258fe150f3d94abf7ae103` |
| 3.12.6 (build 43) | [Download APK](MySpace-v3.12.6-43-lite-dist.apk) | 2075544 bytes | `d68d0e1011d82445964efa1f564bca6a97d5905393154cbba1486a1d08d188e4` |
| 3.12.7 (build 44) | [Download APK](MySpace-v3.12.7-44-lite-dist.apk) | 2076884 bytes | `ff52331518b71a153abf6ef76c964538044121230e1ae87d2855e535a94e0a23` |
| 3.12.8 (build 45) | [Download APK](MySpace-v3.12.8-45-lite-dist.apk) | 2078376 bytes | `b610cef4123e8f17c7bcbed16a47e488d1d299c3951cc250af78ab58aebb9eec` |
| 3.12.9 (build 46) | [Download APK](MySpace-v3.12.9-46-lite-dist.apk) | 2078484 bytes | `f491fec8de4209e6e54d853b211df8a5be62e28f9bb84377267d860ff10c5e0a` |
| 3.12.10 (build 47) | [Download APK](MySpace-v3.12.10-47-lite-dist.apk) | 2078516 bytes | `5cafb05a9871215e3a6516530d3c37e2495db56a7508a14805b063ab7100469d` |
| 3.12.11 (build 48) | [Download APK](MySpace-v3.12.11-48-lite-dist.apk) | 2081960 bytes | `848a08862d00645d4363a1e789cb62ca41e17260fca8bf6150fe1a7bc70ce50d` |
| 3.12.12 (build 49) | [Download APK](MySpace-v3.12.12-49-lite-dist.apk) | 2084620 bytes | `0d1ac3907a4343fe4fe11c99f1b60bc4b9e326d5aa9f11f0516a883c0e8634ca` |
| 3.12.13 (build 50) | [Download APK](MySpace-v3.12.13-50-lite-dist.apk) | 2085112 bytes | `693e0d7791e33470d8169f69b4f344c75e67b1a7df80d8e1a9b9c07932a0e6b5` |
| 3.12.14 (build 51) | [Download APK](MySpace-v3.12.14-51-lite-dist.apk) | 2086112 bytes | `751b238c3b6f89e3a9935ef251311f21191892a3ddf3ba36bd29d3dbb241c05a` |
| 3.12.15 (build 52) | [Download APK](MySpace-v3.12.15-52-lite-dist.apk) | 2087020 bytes | `53d5a2de7f0e9fed6c70be57e259f7784b27a7d6c069ce8e60a824001343536d` |
| 3.12.16 (build 53) | [Download APK](MySpace-v3.12.16-53-lite-dist.apk) | 2087608 bytes | `33d646f99b70a229cf498358729c0b4a01d78e68df43930390143d4e8939a8ea` |
| 3.12.17 (build 54) | [Download APK](MySpace-v3.12.17-54-lite-dist.apk) | 2087820 bytes | `478581be0a1b14a53bb0044652e8a658b30348fe1e78e2cfd8458a54715e9758` |
| 3.12.18 (build 55) | [Download APK](MySpace-v3.12.18-55-lite-dist.apk) | 2088900 bytes | `cd56a2b654dbe7d3a54c4dd291a06e2ec9d7150b6c399a89f93591b167d6e4e8` |
| 3.12.19 (build 56) | [Download APK](MySpace-v3.12.19-56-lite-dist.apk) | 2089128 bytes | `bcdd1de4606348d1e318452002f9f9a4c28bc464af1fabeb9941ee22891da02a` |
| 3.12.20 (build 57) | [Download APK](MySpace-v3.12.20-57-lite-dist.apk) | 2089380 bytes | `74cbf8cda447f7c073f2a9b7d667634e7b06122e84da5a76f8f5dec29acaa2ca` |
| 3.12.21 (build 58) | [Download APK](MySpace-v3.12.21-58-lite-dist.apk) | 2267684 bytes | `629442fc1d54508690f8c75664ea8be7611fef14a74f4edcf6f2bd1cab40068d` |
| 3.12.22 (build 59) | [Download APK](MySpace-v3.12.22-59-lite-dist.apk) | 2268008 bytes | `6fc0270d52cd9c6852afd8ad11a058582c11f0b2a86930daf7a727cf37e79b00` |
| 3.12.23 (build 60) | [Download APK](MySpace-v3.12.23-60-lite-dist.apk) | 2269828 bytes | `992b0e126d57a4cf622583b9784322d5063ef8865d1d02f6f1466b1b8bca987b` |
| 3.12.24 (build 61) | [Download APK](MySpace-v3.12.24-61-lite-dist.apk) | 2270160 bytes | `bd6f3850b659ee8c9c37c17055e9d688caa7ca5c298a28487c475ae0c948e986` |
| 3.12.25 (build 62) | [Download APK](MySpace-v3.12.25-62-lite-dist.apk) | 2270148 bytes | `c99d74c4fe8afdcecfe7ef9b0b1678d897ff1e7a15caafd6779387c6c1b7a746` |
| 3.12.26 (build 63) | [Download APK](MySpace-v3.12.26-63-lite-dist.apk) | 2270592 bytes | `c6170401a226e80fa4f894bac4411d4d4ded61b961e510cab711adb902ea8aab` |
| 3.12.27 (build 64) | [Download APK](MySpace-v3.12.27-64-lite-dist.apk) | 2271544 bytes | `04fb07e7429ba9d36f1d082df95f1904ca20e5aeb7d9764eb443e682b3d36564` |
| 3.12.28 (build 65) | [Download APK](MySpace-v3.12.28-65-lite-dist.apk) | 2271724 bytes | `93802e97a1f03df9fa2f9d740715f419b7a500af6c1a4e78b9d56e2090adab65` |
| 3.12.29 (build 66) | [Download APK](MySpace-v3.12.29-66-lite-dist.apk) | 2271664 bytes | `f994e613062bb52f7dd3a40d89e3d3c08037183313d31aa5fd049a11405e6e72` |
| 3.12.30 (build 67) | [Download APK](MySpace-v3.12.30-67-lite-dist.apk) | 2272776 bytes | `8f8ccdcc73b0c4beea380e962a19897d06dda41a12d838ecdebd4ac0f765c40d` |
| 3.12.31 (build 68) | [Download APK](MySpace-v3.12.31-68-lite-dist.apk) | 2273272 bytes | `20a25e0c3cc57b6f4a2cc7085ecd71a2f7800dc0f6279d0626132f834ec5511f` |
| 3.12.32 (build 69) | [Download APK](MySpace-v3.12.32-69-lite-dist.apk) | 2274280 bytes | `4b59e9dacb501265d8af1b23047c4aeeb364415fdf86ff20fbb1045e21cb6663` |
| 3.12.33 (build 70) | [Download APK](MySpace-v3.12.33-70-lite-dist.apk) | 2274620 bytes | `8046b8ff6fec4d7d6a1d56476406900601256baa2fc2a5e4a125294e149a430a` |
| 3.12.34 (build 71) | [Download APK](MySpace-v3.12.34-71-lite-dist.apk) | 2274616 bytes | `1231de7fac90796e2f6e5d20b84a38154faa4ee9ce6cad3843d6656f861fcf51` |

Retained versions: 1.9.9, 1.10.0, 1.11.0, 1.11.1 and 1.11.2. The v1.11.2 build contains the responsive Settings/Alarm Defaults/Widget Studio update, five glass presets and five selectable touch-feedback styles. Runtime/device QA for v1.11.2 remains NOT RUN.

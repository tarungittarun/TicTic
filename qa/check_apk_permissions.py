"""Asserts a sideloadable APK does not declare permissions Play Protect refuses.

Run against a built APK, not against source. Google Play Protect refuses -- not warns about --
any APK installed outside the Play Store that declares READ_SMS or SEND_SMS, and that refusal
happens before any code in the APK runs, so no source-level assertion can observe it. This is
the one check in the project that looks at installability.

The manifest inside an APK is binary XML whose string pool is UTF-16LE, so permission names are
greppable as bytes without aapt. Controls are asserted first: if the scan cannot see permissions
that are definitely declared, the scan is broken and must fail loudly rather than pass quietly.
"""
import sys
import zipfile

def main(path):
    blob = zipfile.ZipFile(path).read("AndroidManifest.xml")

    def present(name):
        return name.encode("utf-16-le") in blob

    for control in ("INTERNET", "WAKE_LOCK"):
        if not present(control):
            print("SCAN BROKEN: %s should be declared but was not found" % control)
            return 2
    # AGP omits android:debuggable from the merged manifest when it is false, so the attribute
    # name appearing in the binary string pool at all means the build is debuggable -- which is
    # what Play Protect's harmful-app verdict was correctly objecting to.
    if "debuggable".encode("utf-16-le") in blob:
        print("DEBUGGABLE: the APK can be attached to over adb; do not hand this to a tester")
        return 1
    for forbidden in ("READ_SMS", "SEND_SMS"):
        if present(forbidden):
            print("REFUSED AT INSTALL: %s is declared; Play Protect blocks sideloaded APKs "
                  "that carry it" % forbidden)
            return 1
    print("OK: no SMS permissions declared; scan validated by controls")
    return 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))

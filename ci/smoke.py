"""Synthetic-data smoke test for jumpa on an emulator.

Drives the real UI over adb and saves screenshots into ci/shots. Covers the
demo people list, a person detail with affiliation history, notes and
promises, adding a person through its dialog, archive and restore, and the
demo-mode toggle. The contact picker is a system UI and is not driven here.
"""
import re, subprocess, sys, time
import xml.etree.ElementTree as ET

PKG = "com.vanipy.jumpa"
OUT = "ci/shots"
subprocess.run(["mkdir", "-p", OUT])


def adb(*a, **k):
    return subprocess.run(["adb", *a], capture_output=True, **k)


def shot(name):
    time.sleep(1.0)
    data = adb("exec-out", "screencap", "-p").stdout
    open(f"{OUT}/{name}.png", "wb").write(data)


def nodes():
    last = ""
    for attempt in range(15):
        adb("shell", "rm", "-f", "/sdcard/ui.xml")
        dump = adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        raw = adb("exec-out", "cat", "/sdcard/ui.xml")
        xml = raw.stdout.decode("utf-8", "replace")
        try:
            if dump.returncode or raw.returncode:
                raise ET.ParseError("UI dump command failed")
            return [n.attrib for n in ET.fromstring(xml).iter("node")]
        except ET.ParseError:
            last = repr(xml) + dump.stdout.decode("utf-8", "replace") + dump.stderr.decode("utf-8", "replace")
            time.sleep(2)
    open(f"{OUT}/ui-dump-failure.txt", "w").write(last)
    shot("FAILED-ui-dump")
    raise RuntimeError("No valid UIAutomator XML; raw diagnostic saved")


def center(b):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", b)
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def find(text, exact=False):
    for n in nodes():
        t = ((n.get("text") or "") + " " + (n.get("content-desc") or "")).strip().lower()
        if (t == text.lower()) if exact else (text.lower() in t):
            return n
    return None


def present(text, exact=False):
    return find(text, exact) is not None


def tap(text, exact=False, tries=6):
    for _ in range(tries):
        n = find(text, exact)
        if n:
            x, y = center(n["bounds"])
            adb("shell", "input", "tap", str(x), str(y))
            time.sleep(0.8)
            return
        adb("shell", "input", "swipe", "540", "1700", "540", "700", "300")
        time.sleep(1)
    shot("FAILED-" + re.sub(r"\W+", "_", text)[:30])
    print("NOT FOUND:", text)
    sys.exit(1)


def longpress(text, exact=False):
    n = find(text, exact)
    if not n:
        shot("FAILED-lp-" + re.sub(r"\W+", "_", text)[:30])
        print("NOT FOUND (long press):", text)
        sys.exit(1)
    x, y = center(n["bounds"])
    adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "1200")
    time.sleep(1)


def need(text, exact=False):
    for i in range(6):
        if present(text, exact):
            return
        if i >= 2:
            adb("shell", "input", "swipe", "540", "700", "540", "1800", "300")
        time.sleep(1)
    shot("FAILED-need-" + re.sub(r"\W+", "_", text)[:30])
    print("EXPECTED ON SCREEN:", text)
    sys.exit(1)


def type_text(s):
    adb("shell", "input", "text", s.replace(" ", "%s"))
    time.sleep(0.5)


# fresh start
adb("shell", "pm", "clear", PKG)
adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
# cold emulator starts vary a lot; give the first screen a patient window
for _ in range(40):
    if present("demo people"):
        break
    time.sleep(2)
else:
    shot("FAILED-launch")
    print("app did not reach the demo list")
    sys.exit(1)

# demo list: banner, seed people, affiliation subtitle
need("demo people")
need("Nadia Kusuma")
need("product designer at PT Maju Bersama")
need("1 promise open")
shot("01-list-demo")

# person detail: affiliation history, note, promise
tap("Nadia Kusuma")
need("companies and organizations")
need("CV Kreatif Nusantara")
need("2021-2022")
need("Met at a design meetup")
need("Send her the accessibility checklist")
shot("02-person-detail")

# link the seeded phone contact through the system picker
tap("link a contact")
time.sleep(2)
shot("03a-contact-picker")
tap("Uji")
time.sleep(2)
need("Uji")
need("linked read-only", exact=False)
need("unlink")
shot("03b-contact-linked")

# unlink again so the stored state stays clean
tap("unlink")
need("link a contact")

# add a promise through the dialog
tap("+ promise")
need("new promise")
tap("what did you promise?")
type_text("Share the meetup slides")
tap("add", exact=True)
need("Share the meetup slides")
shot("03c-promise-added")

# back to list, add a person
adb("shell", "input", "keyevent", "4")
time.sleep(1)
tap("+ person")
need("new person")
tap("name", exact=True)
type_text("Dewi Anggraini")
tap("add", exact=True)
need("Dewi Anggraini")
shot("04-person-added")

# archive her, verify she moves, restore
longpress("Dewi Anggraini")
time.sleep(1)
if present("Dewi Anggraini"):
    print("archived person still on the active list")
    sys.exit(1)
tap("archive", exact=True)
need("Dewi Anggraini")
shot("05-archive")
longpress("Dewi Anggraini")
time.sleep(1)
tap("active", exact=True)
need("Dewi Anggraini")

# demo off: real file is empty and separate
tap("demo: on")
need("no people yet")
if present("Nadia Kusuma"):
    print("demo data leaked into real mode")
    sys.exit(1)
shot("06-real-empty")
tap("demo: off")
need("Nadia Kusuma")

print("jumpa smoke OK")

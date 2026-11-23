# jumpa

**jumpa** (Indonesian for "meet") is a private relationship notebook for
Android: a Farley file for everyday life. For each person, keep the context
your phone's contact app forgets - which company they work at now, which
ones came before, which organization they represent, what you talked about
last time, and what you promised them.

GPL-3.0. Package `com.vanipy.jumpa`. Signed APKs with SHA-256 sidecars are
on the [releases page](https://github.com/irvan-putra/jumpa/releases).

## What it does today (v0.1.1, prototype)

- People with **multiple dated affiliations**: company or organization,
  role, kind (employer, client, community, ...), start and end. An empty end
  means current, so a career history builds up naturally instead of
  overwriting the last employer.
- **Notes** with dates and **promises/follow-ups** with optional due dates.
- **Read-only contact linking**: link a person to a phone contact through
  the system contact picker. jumpa asks for **zero Android permissions** -
  the picker hands back a one-off read grant for the one contact you pick.
  jumpa never creates, edits or deletes phone contacts.
- **Demo mode** with fictional people, switchable in-app; real data lives in
  a separate file and is never mixed with the demo set.
- Archive, never delete: long-press a person to archive or restore them.

## Web version

<https://vanipy.com/jumpa/> runs in any browser: people, dated affiliations,
notes, promises, archive, and a separate demo set (starts in demo mode with
three invented people). Data stays in the browser's local storage, with plain
JSON export and copy-only import. Not in the web version: phone-contact
linking, the Kunjung handoff, and any sync with the Android app (separate
data). Source: `web/`; logic tests: `node tests/web-core.test.js`.

## Honest limits

- The system picker's read grant is temporary. Reopening a linked contact
  later can fail on some devices; jumpa keeps the linked name and says so
  instead of pretending. Unlink and pick again if that happens.
- Dates are loose text (`2024` or `2024-06`), not real calendar dates, so
  there are no automatic reminders yet.
- Data stays on one device: no sync, no backup, no export yet.
- Prototype UX: screens and gestures follow the family rules (whole-screen
  list, archive-not-delete) but are still first-pass.

## Privacy

No network code, no accounts, no permissions, no analytics. Everything lives
in the app's private storage, unencrypted (device storage is encrypted by
Android). Nothing is uploaded, ever.

## Design notes

- Inspiration: the Farley file (politicians' notes on everyone they met) and
  the follow-through spirit of *The Pursuit of Happyness*. Relationship
  memory, not a sales pipeline.
- Screens: whole-screen lists, no filler headings; long-press removes;
  archived items stay recoverable.
- Icon: two people meeting, family badge rules (numbers-only version strip).

## Build

    ./gradlew testDebugUnitTest assembleDebug

CI runs the same steps on every push (`.github/workflows/build.yml`).
Releases are signed with the jumpa signing key and published as
pre-releases with a `.sha256` sidecar per APK.

## Kunjung handoff prototype (0.1.1)

A separate synthetic encounter page in
[Kunjung](https://github.com/irvan-putra/kunjung) can open
`jumpa://encounter/demo-v1`.
Jumpa shows an editable preview, then lets you choose an existing **demo** person
(including archived people, who stay archived) or create a new demo person.
Only **Save to demo people** commits a dated note. Cancel writes no encounter.
The current real/demo display setting is not changed. Real people are never read
or written by this receiver. Opening a preview on a fresh install does not seed
or write the demo file until Save.

This is a fixed fictional demo, not a live Delta Chat client or a general import.
It accepts no payload, query parameters, invite tokens or real contacts. Context
is labeled as arrival context, not evidence of attendance, unless you explicitly
check the confirmation box. Reopening the same demo cannot add a duplicate.
No auto-merge, sync, contact write, affiliation overwrite or automatic reminder.
Existing 0.1.0 cannot open this receiver; use 0.1.1 or newer.

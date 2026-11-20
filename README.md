# jumpa

**jumpa** (Indonesian for "meet") is a private relationship notebook for
Android: a Farley file for everyday life. For each person, keep the context
your phone's contact app forgets - which company they work at now, which
ones came before, which organization they represent, what you talked about
last time, and what you promised them.

GPL-3.0 (proposed, pending owner's confirmation). Package `com.vanipy.jumpa`.

## What it does today (v0.1.0, prototype)

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
Signed releases follow once the jumpa signing key is set up.

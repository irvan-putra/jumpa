// Synthetic-only tests for web/core.js. Run: node tests/web-core.test.js
const assert = require("assert");
const C = require("../web/core.js");
function mem() { const m = {}; return { getItem: k => (k in m ? m[k] : null), setItem: (k, v) => { m[k] = String(v); }, removeItem: k => { delete m[k]; }, _m: m }; }
let n = 0; const t = (name, f) => { f(); n++; console.log("ok -", name); };

t("starts in demo mode with the three fictional people", () => {
  const s = mem(); assert.equal(C.mode(s), "demo");
  const p = C.load(s, "demo"); assert.deepEqual(p.map(x => x.name), ["Nadia Kusuma", "Bagus Wibowo", "Sinta Prameswari"]);
});
t("real and demo data live under separate keys", () => {
  const s = mem(); C.save(s, "real", [C.cleanPerson({ name: "Test Person" })]);
  assert.equal(C.load(s, "real").length, 1); assert.equal(C.load(s, "demo").length, 3);
  assert.ok(C.KEYS.real !== C.KEYS.demo);
});
t("real mode starts empty and demo is not seeded into it", () => assert.equal(C.load(mem(), "real").length, 0));
t("date rule matches the Android app", () => {
  for (const ok of ["", "2024", "2024-06"]) assert.ok(C.dateOk(ok));
  for (const bad of ["24", "2024-13", "2024-6", "2024-06-01", "soon"]) assert.ok(!C.dateOk(bad));
});
t("affiliation range and description", () => {
  assert.equal(C.range({ start: "2019", end: "2022" }), "2019-2022");
  assert.equal(C.range({ start: "2023", end: "" }), "2023-");
  assert.equal(C.range({ start: "", end: "2022" }), "2022");
  assert.equal(C.describe({ org: "PT X", role: "designer", kind: "employer" }), "designer at PT X (employer)");
  assert.equal(C.describe({ org: "Club", role: "", kind: "" }), "Club");
});
t("current affiliation is the one with an empty end", () => {
  const nadia = C.demoPeople()[0]; assert.equal(C.currentAffiliation(nadia).org, "PT Maju Bersama");
});
t("open promises ignore done ones", () => {
  const [n, , s] = C.demoPeople(); assert.equal(C.openPromises(n), 1); assert.equal(C.openPromises(s), 0);
});
t("import is copy-only: new ids, nothing overwritten", () => {
  const existing = C.demoPeople();
  const out = C.importJson(C.exportJson(existing, "demo"), existing);
  assert.equal(out.people.length, 6); assert.equal(out.added, 3);
  assert.equal(new Set(out.people.map(p => p.id)).size, 6);
  assert.deepEqual(out.people.slice(0, 3), existing);
});
t("import rejects junk and drops bad entries", () => {
  assert.ok(C.importJson("not json", []).error);
  assert.ok(C.importJson('{"a":1}', []).error);
  const o = C.importJson(JSON.stringify([{ name: "  " }, { name: "Ok", affiliations: [{ org: "A", start: "bad" }], notes: [{ text: "" }], promises: [{ text: "x", due: "zzz" }] }, 5]), []);
  assert.equal(o.added, 1); assert.equal(o.skipped, 2);
  assert.equal(o.people[0].affiliations[0].start, ""); assert.equal(o.people[0].notes.length, 0); assert.equal(o.people[0].promises[0].due, "");
});
t("corrupt storage falls back instead of throwing", () => {
  const s = mem(); s.setItem(C.KEYS.real, "{{"); assert.deepEqual(C.load(s, "real"), []);
});
t("search covers name, organization, notes and promises", () => {
  const [n, b] = C.demoPeople();
  assert.ok(C.matches(n, "maju")); assert.ok(C.matches(n, "accessibility")); assert.ok(C.matches(n, "checklist"));
  assert.ok(!C.matches(b, "maju")); assert.ok(C.matches(b, ""));
});
t("reset demo restores the seed", () => {
  const s = mem(); C.save(s, "demo", []); assert.equal(C.load(s, "demo").length, 0); C.resetDemo(s); assert.equal(C.load(s, "demo").length, 3);
});
console.log(n + " tests passed");

t("current affiliation is the NEWEST current one, matching Android Model.java", () => {
  const p = { affiliations: [{ role: "old", start: "2020" }, { role: "new", start: "2024" }] };
  assert.equal(C.currentAffiliation(p).role, "new");
  const q = { affiliations: [{ role: "new", start: "2024" }, { role: "old", start: "2020" }] };
  assert.equal(C.currentAffiliation(q).role, "new");
  assert.equal(C.currentAffiliation({ affiliations: [{ role: "ended", start: "2024", end: "2025" }] }), null);
  assert.equal(C.currentAffiliation({ affiliations: [{ role: "nostart" }, { role: "started", start: "2019" }] }).role, "started");
});

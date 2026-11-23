/* jumpa web core: data model, demo set, validation, import/export.
   No DOM here, so node can test it. Dates are loose text: YYYY or YYYY-MM. */
(function (root) {
  "use strict";
  var KEYS = { demo: "jumpa.web.v1.demo", real: "jumpa.web.v1.real", mode: "jumpa.web.v1.mode" };
  var MAXTEXT = 2000, MAXPEOPLE = 5000;

  function today(d) { d = d || new Date(); return d.getFullYear() + "-" + String(d.getMonth() + 1).padStart(2, "0") + "-" + String(d.getDate()).padStart(2, "0"); }
  function dateOk(t) { return !t || /^\d{4}(-(0[1-9]|1[0-2]))?$/.test(t); }
  function newId() {
    if (root.crypto && root.crypto.randomUUID) return root.crypto.randomUUID();
    return "p" + Date.now().toString(36) + Math.random().toString(36).slice(2, 10);
  }
  function range(a) {
    var cur = !a.end;
    if (!a.start && !cur) return a.end;
    if (!a.start) return "";
    return cur ? a.start + "-" : a.start + "-" + a.end;
  }
  function describe(a) {
    var core = a.role && a.org ? a.role + " at " + a.org : (a.org || a.role || "");
    return a.kind ? core + " (" + a.kind + ")" : core;
  }
  function currentAffiliation(p) {
    for (var i = 0; i < p.affiliations.length; i++) if (!p.affiliations[i].end) return p.affiliations[i];
    return null;
  }
  function openPromises(p) { return p.promises.filter(function (x) { return !x.done; }).length; }

  function str(v, max) { return typeof v === "string" ? v.slice(0, max || MAXTEXT) : ""; }
  function cleanPerson(raw, keepId) {
    if (!raw || typeof raw !== "object") return null;
    var name = str(raw.name, 200).trim();
    if (!name) return null;
    var p = { id: keepId && typeof raw.id === "string" && raw.id ? raw.id.slice(0, 80) : newId(), name: name, archived: raw.archived === true, updated: Number(raw.updated) || Date.now(), affiliations: [], notes: [], promises: [] };
    (Array.isArray(raw.affiliations) ? raw.affiliations : []).slice(0, 100).forEach(function (a) {
      if (!a || typeof a !== "object") return;
      var x = { org: str(a.org, 200), role: str(a.role, 200), kind: str(a.kind, 60), start: str(a.start, 7), end: str(a.end, 7) };
      if (!dateOk(x.start)) x.start = ""; if (!dateOk(x.end)) x.end = "";
      if (x.org || x.role) p.affiliations.push(x);
    });
    (Array.isArray(raw.notes) ? raw.notes : []).slice(0, 1000).forEach(function (n) {
      if (n && typeof n === "object" && str(n.text).trim()) p.notes.push({ text: str(n.text), date: str(n.date, 10) });
    });
    (Array.isArray(raw.promises) ? raw.promises : []).slice(0, 1000).forEach(function (n) {
      if (n && typeof n === "object" && str(n.text).trim()) p.promises.push({ text: str(n.text), due: dateOk(str(n.due, 7)) ? str(n.due, 7) : "", done: n.done === true });
    });
    return p;
  }

  /* Fictional people only. No real names, no real companies. */
  function demoPeople() {
    var now = Date.now();
    return [
      { id: "demo-nadia", name: "Nadia Kusuma", updated: now,
        affiliations: [{ org: "PT Maju Bersama", role: "product designer", kind: "employer", start: "2024", end: "" }, { org: "CV Kreatif Nusantara", role: "junior designer", kind: "employer", start: "2021", end: "2022" }],
        notes: [{ date: "2026-10-02", text: "Met at a design meetup. Cares about accessibility; asked about our testing setup." }],
        promises: [{ text: "Send her the accessibility checklist", due: "2026-11", done: false }] },
      { id: "demo-bagus", name: "Bagus Wibowo", updated: now,
        affiliations: [{ org: "Komunitas Sepeda Pagi", role: "coordinator", kind: "community", start: "2023", end: "" }],
        notes: [{ date: "2026-09-14", text: "Prefers voice notes over text. Planning a December charity ride." }], promises: [] },
      { id: "demo-sinta", name: "Sinta Prameswari", updated: now,
        affiliations: [{ org: "Yayasan Cahaya", role: "volunteer lead", kind: "organization", start: "2025", end: "" }],
        notes: [], promises: [{ text: "Introduce her to the venue committee", due: "", done: true }] }
    ].map(function (p) { p.archived = false; return p; });
  }

  function mode(storage) { return storage.getItem(KEYS.mode) === "real" ? "real" : "demo"; } // starts in demo, like the app
  function setMode(storage, m) { storage.setItem(KEYS.mode, m === "real" ? "real" : "demo"); }
  function load(storage, m) {
    var raw = storage.getItem(KEYS[m]);
    if (raw === null) return m === "demo" ? demoPeople() : [];
    try { var a = JSON.parse(raw); return (Array.isArray(a) ? a : []).map(function (x) { return cleanPerson(x, true); }).filter(Boolean); }
    catch (e) { return m === "demo" ? demoPeople() : []; }
  }
  function save(storage, m, people) { storage.setItem(KEYS[m], JSON.stringify(people)); }
  function resetDemo(storage) { storage.removeItem(KEYS.demo); }

  function exportJson(people, m) {
    return JSON.stringify({ app: "jumpa-web", version: 1, mode: m, exported: today(), people: people }, null, 2);
  }
  /* Copy-only import: every imported person gets a new id and is appended; nothing is overwritten. */
  function importJson(text, existing) {
    var data;
    try { data = JSON.parse(text); } catch (e) { return { error: "That file is not valid JSON." }; }
    var list = Array.isArray(data) ? data : (data && Array.isArray(data.people) ? data.people : null);
    if (!list) return { error: "No people found in that file." };
    if (existing.length + list.length > MAXPEOPLE) return { error: "Too many people." };
    var added = list.map(function (x) { return cleanPerson(x, false); }).filter(Boolean);
    if (!added.length) return { error: "No usable people in that file." };
    return { people: existing.concat(added), added: added.length, skipped: list.length - added.length };
  }
  function sortPeople(list) {
    return list.slice().sort(function (a, b) { return a.name.localeCompare(b.name); });
  }
  function matches(p, q) {
    q = q.trim().toLowerCase(); if (!q) return true;
    var hay = [p.name].concat(p.affiliations.map(function (a) { return a.org + " " + a.role + " " + a.kind; }), p.notes.map(function (n) { return n.text; }), p.promises.map(function (n) { return n.text; })).join(" ").toLowerCase();
    return hay.indexOf(q) >= 0;
  }

  var api = { KEYS: KEYS, today: today, dateOk: dateOk, newId: newId, range: range, describe: describe, currentAffiliation: currentAffiliation, openPromises: openPromises, cleanPerson: cleanPerson, demoPeople: demoPeople, mode: mode, setMode: setMode, load: load, save: save, resetDemo: resetDemo, exportJson: exportJson, importJson: importJson, sortPeople: sortPeople, matches: matches };
  if (typeof module !== "undefined" && module.exports) module.exports = api; else root.JumpaCore = api;
})(typeof window !== "undefined" ? window : globalThis);

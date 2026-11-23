(function () {
  "use strict";
  var C = window.JumpaCore, root = document.getElementById("app");
  var store; try { store = window.localStorage; store.setItem("jumpa.probe", "1"); store.removeItem("jumpa.probe"); }
  catch (e) { var mem = {}; store = { getItem: function (k) { return k in mem ? mem[k] : null; }, setItem: function (k, v) { mem[k] = String(v); }, removeItem: function (k) { delete mem[k]; } }; }
  var persistent = store === window.localStorage;
  var mode = C.mode(store), people = C.load(store, mode), showArchived = false, query = "", msg = "";

  function h(tag, attrs) {
    var el = document.createElement(tag);
    for (var k in attrs || {}) {
      if (k === "class") el.className = attrs[k]; else if (k.slice(0, 2) === "on") el.addEventListener(k.slice(2), attrs[k]);
      else if (attrs[k] !== false && attrs[k] != null) el.setAttribute(k, attrs[k]);
    }
    for (var i = 2; i < arguments.length; i++) { var c = arguments[i]; if (c == null || c === false) continue; el.appendChild(typeof c === "string" ? document.createTextNode(c) : c); }
    return el;
  }
  function persist() { C.save(store, mode, people); }
  function find(id) { return people.filter(function (p) { return p.id === id; })[0]; }
  function touch(p) { p.updated = Date.now(); persist(); }
  function go(hash) { if (location.hash === hash) render(); else location.hash = hash; }
  function field(label, input) { return h("div", null, h("label", null, label), input); }

  function modeBar() {
    var demo = mode === "demo";
    var bar = h("div", { class: "row" },
      h("button", { "aria-pressed": String(demo), onclick: function () { setMode("demo"); } }, "Demo"),
      h("button", { "aria-pressed": String(!demo), onclick: function () { setMode("real"); } }, "My people"));
    var out = [bar];
    out.push(h("div", { class: "banner" }, demo ? "Demo mode: three invented people. Changes here never touch your own list." : "Your own list. Saved in this browser only."));
    if (!persistent) out.push(h("div", { class: "banner" }, "This browser blocks local storage, so nothing is saved after you close the page. Export JSON to keep your work."));
    return out;
  }
  function setMode(m) { mode = m; C.setMode(store, m); people = C.load(store, m); query = ""; msg = ""; go("#/"); }

  function listView() {
    var kids = modeBar();
    var q = h("input", { type: "search", placeholder: "Search names, organizations, notes", value: query, "aria-label": "Search", oninput: function (e) { query = e.target.value; fill(); } });
    kids.push(q);
    kids.push(h("div", { class: "row" },
      h("button", { class: "primary", onclick: addPerson }, "Add person"),
      h("button", { "aria-pressed": String(showArchived), onclick: function () { showArchived = !showArchived; render(); } }, "Show archived")));
    var listEl = h("div", { id: "list" });
    kids.push(listEl);
    var tools = h("div", { class: "row" },
      h("button", { onclick: doExport }, "Export JSON"),
      h("label", { class: "btn", style: "margin:0;color:inherit;font-size:1rem" }, "Import JSON", h("input", { type: "file", accept: "application/json,.json", style: "display:none", onchange: doImport })));
    if (mode === "demo") tools.appendChild(h("button", { onclick: function () { C.resetDemo(store); people = C.load(store, "demo"); msg = "Demo people restored."; render(); } }, "Reset demo"));
    kids.push(tools);
    if (msg) kids.push(h("div", { class: "banner", role: "status" }, msg));
    function fill() {
      listEl.textContent = "";
      var shown = C.sortPeople(people).filter(function (p) { return (showArchived || !p.archived) && C.matches(p, query); });
      if (!shown.length) listEl.appendChild(h("div", { class: "sub" }, people.length ? "No one matches." : "No people yet. Tap Add person."));
      shown.forEach(function (p) {
        var cur = C.currentAffiliation(p), open = C.openPromises(p);
        var sub = (cur ? C.describe(cur) : (p.affiliations[0] ? C.describe(p.affiliations[0]) : "No affiliation yet")) + (open ? " · " + open + " open promise" + (open > 1 ? "s" : "") : "");
        listEl.appendChild(h("button", { class: "card person", onclick: function () { go("#/p/" + encodeURIComponent(p.id)); } },
          h("b", null, p.name, p.archived ? h("span", { class: "tag" }, "archived") : null), h("span", { class: "sub" }, sub)));
      });
    }
    fill();
    return kids;
  }

  function addPerson() {
    var name = (window.prompt("Name") || "").trim();
    if (!name) return;
    var p = C.cleanPerson({ name: name }, false); people.push(p); persist(); go("#/p/" + encodeURIComponent(p.id));
  }
  function doExport() {
    var blob = new Blob([C.exportJson(people, mode)], { type: "application/json" });
    var a = h("a", { href: URL.createObjectURL(blob), download: "jumpa-" + mode + "-" + C.today() + ".json" });
    document.body.appendChild(a); a.click(); a.remove(); msg = "Exported " + people.length + " people."; render();
  }
  function doImport(e) {
    var f = e.target.files && e.target.files[0]; if (!f) return;
    var r = new FileReader();
    r.onload = function () {
      var out = C.importJson(String(r.result), people);
      if (out.error) msg = out.error; else { people = out.people; persist(); msg = "Added " + out.added + " people" + (out.skipped ? " (" + out.skipped + " skipped)" : "") + ". No one was overwritten."; }
      render();
    };
    r.readAsText(f);
  }

  function personView(id) {
    var p = find(id);
    if (!p) return [h("p", null, "That person is not in this list."), h("button", { onclick: function () { go("#/"); } }, "Back")];
    var kids = [h("div", { class: "row" }, h("button", { onclick: function () { go("#/"); } }, "← People"), h("span", { class: "tag" }, mode === "demo" ? "demo" : "my people"))];
    var name = h("input", { value: p.name, "aria-label": "Name", onchange: function (e) { var v = e.target.value.trim(); if (v) { p.name = v; touch(p); } else e.target.value = p.name; } });
    kids.push(h("div", { class: "card" }, field("Name", name),
      h("div", { class: "row" }, h("button", { onclick: function () { p.archived = !p.archived; touch(p); render(); } }, p.archived ? "Restore" : "Archive"))));

    kids.push(h("h2", null, "Affiliations"));
    p.affiliations.forEach(function (a, i) {
      kids.push(h("div", { class: "card" }, h("b", null, C.describe(a)), h("div", { class: "sub" }, (C.range(a) || "dates unknown") + (a.end ? "" : " · current")),
        h("div", { class: "row" }, h("button", { onclick: function () { if (confirm("Remove this affiliation?")) { p.affiliations.splice(i, 1); touch(p); render(); } } }, "Remove"))));
    });
    var org = h("input", { id: "a-org" }), role = h("input", { id: "a-role" }), kind = h("input", { id: "a-kind", placeholder: "employer, client, community" }),
      st = h("input", { id: "a-start", placeholder: "2024 or 2024-06", inputmode: "numeric" }), en = h("input", { id: "a-end", placeholder: "empty = current", inputmode: "numeric" }), err = h("div", { class: "err", role: "alert" });
    kids.push(h("details", null, h("summary", null, "Add affiliation"), field("Company or organization", org), field("Role", role), field("Kind", kind),
      h("div", { class: "grid2" }, field("Start", st), field("End", en)), err,
      h("div", { class: "row" }, h("button", { class: "primary", onclick: function () {
        var a = { org: org.value.trim(), role: role.value.trim(), kind: kind.value.trim(), start: st.value.trim(), end: en.value.trim() };
        if (!a.org && !a.role) { err.textContent = "Add an organization or a role."; return; }
        if (!C.dateOk(a.start) || !C.dateOk(a.end)) { err.textContent = "Dates must look like 2024 or 2024-06."; return; }
        p.affiliations.push(a); touch(p); render(); } }, "Save affiliation"))));

    kids.push(h("h2", null, "Notes"));
    p.notes.slice().sort(function (a, b) { return b.date.localeCompare(a.date); }).forEach(function (n) { kids.push(h("div", { class: "card" }, h("div", { class: "sub" }, n.date || "undated"), h("div", null, n.text))); });
    var nt = h("textarea", { "aria-label": "New note", placeholder: "What did you talk about?" });
    kids.push(h("div", { class: "card" }, nt, h("div", { class: "row" }, h("button", { class: "primary", onclick: function () { var t = nt.value.trim(); if (!t) return; p.notes.push({ text: t, date: C.today() }); touch(p); render(); } }, "Add note"))));

    kids.push(h("h2", null, "Promises"));
    p.promises.forEach(function (x) {
      var cb = h("input", { type: "checkbox", style: "width:auto;min-height:0;margin-right:.5rem", onchange: function () { x.done = cb.checked; touch(p); render(); } }); cb.checked = x.done;
      kids.push(h("div", { class: "card" }, h("label", { style: "display:flex;align-items:center;margin:0;font-size:1rem;color:inherit" }, cb, h("span", { class: x.done ? "done" : "" }, x.text)), x.due ? h("div", { class: "sub" }, "due " + x.due) : null));
    });
    var pt = h("input", { "aria-label": "New promise", placeholder: "What did you promise?" }), pd = h("input", { "aria-label": "Due", placeholder: "due: 2026-12 (optional)" }), perr = h("div", { class: "err", role: "alert" });
    kids.push(h("div", { class: "card" }, pt, pd, perr, h("div", { class: "row" }, h("button", { class: "primary", onclick: function () {
      var t = pt.value.trim(), d = pd.value.trim(); if (!t) return;
      if (!C.dateOk(d)) { perr.textContent = "Due must look like 2026 or 2026-12."; return; }
      p.promises.push({ text: t, due: d, done: false }); touch(p); render(); } }, "Add promise"))));
    return kids;
  }

  function render() {
    var m = location.hash.match(/^#\/p\/(.+)$/), kids = m ? personView(decodeURIComponent(m[1])) : listView();
    root.textContent = ""; kids.forEach(function (k) { root.appendChild(k); });
    document.getElementById("howto").style.display = m ? "none" : "";
    document.getElementById("limits").style.display = m ? "none" : "";
    document.getElementById("android").style.display = m ? "none" : "";
    msg = "";
  }
  window.addEventListener("hashchange", render);
  render();
})();

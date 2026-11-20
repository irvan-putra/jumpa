package com.vanipy.jumpa;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * jumpa's data: people, their dated affiliations, notes and promises.
 * Dates are free-form "YYYY" or "YYYY-MM" text; an empty end means current.
 */
public final class Model {

    public static String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    /** True when text is empty or matches YYYY or YYYY-MM. */
    public static boolean dateOk(String text) {
        if (text == null || text.isEmpty()) return true;
        return text.matches("\\d{4}(-(0[1-9]|1[0-2]))?");
    }

    public static final class Affiliation {
        public String org = "";
        public String role = "";
        public String kind = "";   // e.g. employer, client, community
        public String start = "";
        public String end = "";    // empty means current

        public boolean isCurrent() {
            return end == null || end.isEmpty();
        }

        /** "2023-" or "2019-2022", empty when nothing is known. */
        public String range() {
            if ((start == null || start.isEmpty()) && !isCurrent()) return end;
            if (start == null || start.isEmpty()) return "";
            return isCurrent() ? start + "-" : start + "-" + end;
        }

        /** "designer at PT Maju Bersama", "member of Komunitas Sepeda". */
        public String describe() {
            String core;
            if (!role.isEmpty() && !org.isEmpty()) core = role + " at " + org;
            else core = !org.isEmpty() ? org : role;
            if (kind.isEmpty()) return core;
            return core + " (" + kind + ")";
        }
    }

    public static final class Note {
        public String text = "";
        public String date = "";
    }

    public static final class Promise {
        public String text = "";
        public String due = "";
        public boolean done;
    }

    public static final class Person {
        public String id = "";
        public String name = "";
        public boolean archived;
        public String contactUri;    // system contact, linked read-only
        public String contactName;
        public long updated;
        public final List<Affiliation> affiliations = new ArrayList<>();
        public final List<Note> notes = new ArrayList<>();
        public final List<Promise> promises = new ArrayList<>();
        public final List<String> encounterIds = new ArrayList<>();

        /** The newest current affiliation, or null when there is none. */
        public Affiliation currentAffiliation() {
            Affiliation best = null;
            for (Affiliation a : affiliations) {
                if (!a.isCurrent()) continue;
                if (best == null || a.start.compareTo(best.start) > 0) best = a;
            }
            return best;
        }

        public int openPromises() {
            int n = 0;
            for (Promise p : promises) if (!p.done) n++;
            return n;
        }

        /** "designer at PT Maju Bersama", or "" when nothing is current. */
        public String subtitle() {
            Affiliation a = currentAffiliation();
            return a == null ? "" : a.describe();
        }
    }

    // ---- org.json persistence (Android runtime only) ----

    public static JSONObject personToJson(Person p) throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", p.id);
        o.put("name", p.name);
        o.put("archived", p.archived);
        o.put("updated", p.updated);
        if (p.contactUri != null) o.put("contactUri", p.contactUri);
        if (p.contactName != null) o.put("contactName", p.contactName);
        JSONArray affs = new JSONArray();
        for (Affiliation a : p.affiliations) {
            JSONObject ao = new JSONObject();
            ao.put("org", a.org);
            ao.put("role", a.role);
            ao.put("kind", a.kind);
            ao.put("start", a.start);
            ao.put("end", a.end);
            affs.put(ao);
        }
        o.put("affiliations", affs);
        JSONArray notes = new JSONArray();
        for (Note n : p.notes) {
            JSONObject no = new JSONObject();
            no.put("text", n.text);
            no.put("date", n.date);
            notes.put(no);
        }
        o.put("notes", notes);
        JSONArray promises = new JSONArray();
        for (Promise pr : p.promises) {
            JSONObject po = new JSONObject();
            po.put("text", pr.text);
            po.put("due", pr.due);
            po.put("done", pr.done);
            promises.put(po);
        }
        o.put("promises", promises);
        o.put("encounterIds", new JSONArray(p.encounterIds));
        return o;
    }

    public static Person personFromJson(JSONObject o) throws JSONException {
        Person p = new Person();
        p.id = o.optString("id", newId());
        p.name = o.optString("name", "");
        p.archived = o.optBoolean("archived", false);
        p.updated = o.optLong("updated", 0);
        p.contactUri = o.has("contactUri") ? o.optString("contactUri", null) : null;
        p.contactName = o.has("contactName") ? o.optString("contactName", null) : null;
        JSONArray affs = o.optJSONArray("affiliations");
        if (affs != null) {
            for (int i = 0; i < affs.length(); i++) {
                JSONObject ao = affs.getJSONObject(i);
                Affiliation a = new Affiliation();
                a.org = ao.optString("org", "");
                a.role = ao.optString("role", "");
                a.kind = ao.optString("kind", "");
                a.start = ao.optString("start", "");
                a.end = ao.optString("end", "");
                p.affiliations.add(a);
            }
        }
        JSONArray notes = o.optJSONArray("notes");
        if (notes != null) {
            for (int i = 0; i < notes.length(); i++) {
                JSONObject no = notes.getJSONObject(i);
                Note n = new Note();
                n.text = no.optString("text", "");
                n.date = no.optString("date", "");
                p.notes.add(n);
            }
        }
        JSONArray promises = o.optJSONArray("promises");
        if (promises != null) {
            for (int i = 0; i < promises.length(); i++) {
                JSONObject po = promises.getJSONObject(i);
                Promise pr = new Promise();
                pr.text = po.optString("text", "");
                pr.due = po.optString("due", "");
                pr.done = po.optBoolean("done", false);
                p.promises.add(pr);
            }
        }
        JSONArray encounterIds = o.optJSONArray("encounterIds");
        if (encounterIds != null) for (int i = 0; i < encounterIds.length(); i++)
            p.encounterIds.add(encounterIds.getString(i));
        return p;
    }
}

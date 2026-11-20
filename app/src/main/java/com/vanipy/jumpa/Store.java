package com.vanipy.jumpa;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Local JSON store. Real data and demo data live in separate files so the
 * demo set can be wiped or shown without touching real people. Nothing
 * leaves the device: no network, no accounts.
 */
public final class Store {
    private static final String PREFS = "jumpa";
    private static final String KEY_DEMO = "demoMode";
    private static final String KEY_SEEDED = "demoSeeded";

    private final Context ctx;

    public Store(Context ctx) {
        this.ctx = ctx.getApplicationContext();
    }

    public boolean demoMode() {
        return prefs().getBoolean(KEY_DEMO, true); // prototype starts in demo
    }

    public void setDemoMode(boolean on) {
        prefs().edit().putBoolean(KEY_DEMO, on).apply();
    }

    private SharedPreferences prefs() {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private File file() {
        return new File(ctx.getFilesDir(), demoMode() ? "people-demo.json" : "people.json");
    }

    public synchronized List<Model.Person> load() {
        if (demoMode() && !prefs().getBoolean(KEY_SEEDED, false)) {
            List<Model.Person> seed = demoPeople();
            save(seed);
            prefs().edit().putBoolean(KEY_SEEDED, true).apply();
            return seed;
        }
        File f = file();
        if (!f.exists()) return new ArrayList<>();
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[(int) f.length()];
            int off = 0;
            while (off < buf.length) {
                int n = in.read(buf, off, buf.length - off);
                if (n < 0) break;
                off += n;
            }
            JSONObject root = new JSONObject(new String(buf, 0, off, StandardCharsets.UTF_8));
            JSONArray arr = root.optJSONArray("people");
            List<Model.Person> out = new ArrayList<>();
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    out.add(Model.personFromJson(arr.getJSONObject(i)));
                }
            }
            return out;
        } catch (Exception e) {
            // A broken file must not wipe the data: keep it, start empty view.
            return new ArrayList<>();
        }
    }

    public synchronized void save(List<Model.Person> people) {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("changed", System.currentTimeMillis());
            JSONArray arr = new JSONArray();
            for (Model.Person p : people) arr.put(Model.personToJson(p));
            root.put("people", arr);
            File tmp = new File(ctx.getFilesDir(), file().getName() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(root.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (!tmp.renameTo(file())) throw new IllegalStateException("rename failed");
        } catch (Exception e) {
            throw new IllegalStateException("could not save people: " + e.getMessage(), e);
        }
    }

    public static Model.Person find(List<Model.Person> people, String id) {
        for (Model.Person p : people) if (p.id.equals(id)) return p;
        return null;
    }

    /** Fictional demo people. No real names, no real companies. */
    private static List<Model.Person> demoPeople() {
        List<Model.Person> out = new ArrayList<>();

        Model.Person nadia = new Model.Person();
        nadia.id = Model.newId();
        nadia.name = "Nadia Kusuma";
        Model.Affiliation a1 = new Model.Affiliation();
        a1.org = "PT Maju Bersama";
        a1.role = "product designer";
        a1.kind = "employer";
        a1.start = "2024";
        nadia.affiliations.add(a1);
        Model.Affiliation a2 = new Model.Affiliation();
        a2.org = "CV Kreatif Nusantara";
        a2.role = "junior designer";
        a2.kind = "employer";
        a2.start = "2021";
        a2.end = "2022";
        nadia.affiliations.add(a2);
        Model.Note n1 = new Model.Note();
        n1.date = "2026-10-02";
        n1.text = "Met at a design meetup. Cares about accessibility; asked about our testing setup.";
        nadia.notes.add(n1);
        Model.Promise pr1 = new Model.Promise();
        pr1.text = "Send her the accessibility checklist";
        pr1.due = "2026-11";
        nadia.promises.add(pr1);
        nadia.updated = System.currentTimeMillis();
        out.add(nadia);

        Model.Person bagus = new Model.Person();
        bagus.id = Model.newId();
        bagus.name = "Bagus Wibowo";
        Model.Affiliation b1 = new Model.Affiliation();
        b1.org = "Komunitas Sepeda Pagi";
        b1.role = "coordinator";
        b1.kind = "community";
        b1.start = "2023";
        bagus.affiliations.add(b1);
        Model.Note n2 = new Model.Note();
        n2.date = "2026-09-14";
        n2.text = "Prefers voice notes over text. Planning a December charity ride.";
        bagus.notes.add(n2);
        bagus.updated = System.currentTimeMillis();
        out.add(bagus);

        Model.Person sinta = new Model.Person();
        sinta.id = Model.newId();
        sinta.name = "Sinta Prameswari";
        Model.Affiliation s1 = new Model.Affiliation();
        s1.org = "Yayasan Cahaya";
        s1.role = "volunteer lead";
        s1.kind = "organization";
        s1.start = "2025";
        sinta.affiliations.add(s1);
        Model.Promise pr2 = new Model.Promise();
        pr2.text = "Introduce her to the venue committee";
        pr2.done = true;
        sinta.promises.add(pr2);
        sinta.updated = System.currentTimeMillis();
        out.add(sinta);

        return out;
    }
}

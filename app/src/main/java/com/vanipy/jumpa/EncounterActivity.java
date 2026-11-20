package com.vanipy.jumpa;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Explicit preview for a fixed fictional encounter. Never opens the real store. */
public class EncounterActivity extends Activity {
    private Store store;
    private List<Model.Person> people;
    private final List<String> personIds = new ArrayList<>();
    private EditText name, context, date, note;
    private CheckBox confirmed;
    private Spinner target;
    private TextView status, save;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Uri uri = getIntent().getData();
        if (uri == null || uri.toString().length() > 512 || !"jumpa".equals(uri.getScheme())
                || !"encounter".equals(uri.getHost()) || !"/demo-v1".equals(uri.getPath())
                || uri.getQuery() != null || uri.getFragment() != null) {
            new AlertDialog.Builder(this).setMessage("Unsupported handoff. This prototype accepts only the fixed Kunjung demo.")
                    .setPositiveButton("close", (d, w) -> finish()).setOnCancelListener(d -> finish()).show();
            return;
        }
        store = Store.demoOnly(this);
        try { people = store.load(); }
        catch (IllegalStateException e) {
            new AlertDialog.Builder(this).setMessage("Could not read demo people. No changes made.")
                    .setPositiveButton("close", (d, w) -> finish()).setOnCancelListener(d -> finish()).show();
            return;
        }
        LinearLayout body = Ui.column(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(body);
        setContentView(scroll);
        TextView cancel = Ui.button(this, "< cancel");
        cancel.setOnClickListener(v -> finish());
        body.addView(cancel);
        body.addView(Ui.heading(this, "Kunjung encounter preview"));
        body.addView(Ui.muted(this, "Synthetic prototype. Saves only to Jumpa demo people, never your real list. Nothing is saved until you confirm."));
        body.addView(Ui.heading(this, "person"));
        target = new Spinner(this);
        List<String> labels = new ArrayList<>();
        labels.add("Create a new demo person");
        personIds.add(null);
        for (Model.Person p : people) {
            labels.add(p.name + (p.archived ? " (archived)" : ""));
            personIds.add(p.id);
        }
        target.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        body.addView(target);
        body.addView(Ui.muted(this, "Choose explicitly. Similar names are not merged. Existing names and affiliations stay unchanged."));
        name = field(body, "name for a new demo person", "Rani Demo", 120, false);
        context = field(body, "arrival context / where met", "Design meetup demo invitation", 300, false);
        confirmed = new CheckBox(this);
        confirmed.setText("I confirm this is where we met (demo)");
        body.addView(confirmed);
        body.addView(Ui.muted(this, "Unchecked means arrival context only. An invitation is not proof that someone attended."));
        date = field(body, "encounter date, YYYY-MM-DD (optional)", "2026-11-20", 10, false);
        note = field(body, "note (optional)", "Fictional encounter for testing the handoff.", 2000, true);
        body.addView(Ui.muted(this, "This first handoff adds one dated note. No contact linking, affiliation changes, promises, messaging or sync."));
        status = Ui.line(this, "");
        status.setTextIsSelectable(true);
        body.addView(status);
        save = Ui.button(this, "Save to demo people");
        save.setOnClickListener(v -> save());
        body.addView(save);
        if (Encounter.alreadySaved(people)) {
            status.setText("This demo encounter is already saved. It will not be added again.");
            save.setEnabled(false);
        }
    }

    private EditText field(LinearLayout body, String label, String value, int max, boolean multi) {
        body.addView(Ui.heading(this, label));
        EditText input = new EditText(this);
        input.setText(value);
        input.setContentDescription(label);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
        input.setSingleLine(!multi);
        if (multi) input.setMinLines(2);
        body.addView(input);
        return input;
    }

    private void save() {
        String n = name.getText().toString().trim();
        String c = context.getText().toString().trim();
        String d = date.getText().toString().trim();
        String text = note.getText().toString().trim();
        int pos = target.getSelectedItemPosition();
        String error = Encounter.validate(pos == 0 ? n : "existing person", c, d, text);
        if (error != null) { status.setText(error); return; }
        // Re-read immediately before committing. Canceled previews write no encounter.
        List<Model.Person> fresh;
        try { fresh = store.load(); }
        catch (IllegalStateException e) { status.setText("Could not read demo people. Nothing saved."); return; }
        if (Encounter.alreadySaved(fresh)) { status.setText("Already saved. No duplicate added."); save.setEnabled(false); return; }
        Model.Person p = pos == 0 ? new Model.Person() : Store.find(fresh, personIds.get(pos));
        if (p == null) { status.setText("That demo person is no longer available. Cancel and open the preview again."); return; }
        if (pos == 0) { p.id = Model.newId(); p.name = n; fresh.add(p); }
        Model.Note entry = new Model.Note();
        entry.date = d;
        entry.text = Encounter.noteText(c, confirmed.isChecked(), text);
        p.notes.add(entry);
        p.encounterIds.add(Encounter.ID);
        p.updated = System.currentTimeMillis();
        try {
            store.save(fresh);
            save.setEnabled(false);
            status.setText("Saved to demo people: " + p.name + ". Real people unchanged. Open Jumpa and turn demo on to see it.");
            new AlertDialog.Builder(this).setTitle("Demo encounter saved")
                    .setMessage("Saved one note to " + p.name + ". Nothing was synced or sent. Your current demo/real setting was not changed.")
                    .setPositiveButton("close", (dialog, which) -> finish()).show();
        } catch (IllegalStateException e) {
            status.setText("Could not save the demo encounter. Nothing marked complete. Try again or cancel.");
        }
    }
}

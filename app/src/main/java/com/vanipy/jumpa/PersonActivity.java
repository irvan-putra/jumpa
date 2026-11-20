package com.vanipy.jumpa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** One person: contact link, dated affiliations, notes, promises. */
public class PersonActivity extends Activity {

    private static final int REQ_PICK_CONTACT = 41;

    private Store store;
    private String personId;
    private LinearLayout body;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        personId = getIntent().getStringExtra("id");

        body = Ui.column(this);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView back = Ui.button(this, "< back");
        back.setOnClickListener(v -> finish());
        back.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), 0, 0);
        root.addView(back);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private Model.Person person(List<Model.Person> people) {
        Model.Person p = Store.find(people, personId);
        if (p == null) {
            Toast.makeText(this, "person not found", Toast.LENGTH_SHORT).show();
            finish();
        }
        return p;
    }

    private void refresh() {
        List<Model.Person> people = store.load();
        Model.Person p = person(people);
        if (p == null) return;
        body.removeAllViews();

        TextView name = Ui.line(this, p.name);
        name.setTextSize(22);
        body.addView(name);

        // --- phone contact link (read-only, via system picker) ---
        body.addView(Ui.heading(this, "phone contact"));
        LinearLayout contactCard = Ui.card(this);
        if (p.contactName != null) {
            contactCard.addView(Ui.line(this, p.contactName));
            contactCard.addView(Ui.muted(this, "linked read-only; jumpa never edits phone contacts"));
            TextView open = Ui.button(this, "open in Contacts");
            open.setOnClickListener(v -> openContact(p));
            TextView unlink = Ui.button(this, "unlink");
            unlink.setOnClickListener(v -> {
                p.contactUri = null;
                p.contactName = null;
                touch(p, people);
            });
            contactCard.addView(open);
            contactCard.addView(unlink);
        } else {
            contactCard.addView(Ui.muted(this,
                    "link a phone contact without giving jumpa your address book: "
                            + "you pick one contact, the system grants one read-only peek"));
            TextView link = Ui.button(this, "link a contact");
            link.setOnClickListener(v -> pickContact());
            contactCard.addView(link);
        }
        body.addView(contactCard);

        // --- affiliations ---
        body.addView(Ui.heading(this, "companies and organizations"));
        for (Model.Affiliation a : p.affiliations) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.line(this, a.describe()));
            String range = a.range();
            card.addView(Ui.muted(this, a.isCurrent()
                    ? (range.isEmpty() ? "current" : "current, since " + a.start)
                    : range));
            card.setOnLongClickListener(v -> {
                p.affiliations.remove(a);
                touch(p, people);
                return true;
            });
            body.addView(card);
        }
        TextView addAff = Ui.button(this, "+ affiliation");
        addAff.setOnClickListener(v -> addAffiliation(p, people));
        body.addView(addAff);

        // --- notes ---
        body.addView(Ui.heading(this, "notes"));
        for (int i = p.notes.size() - 1; i >= 0; i--) {
            Model.Note n = p.notes.get(i);
            LinearLayout card = Ui.card(this);
            card.addView(Ui.line(this, n.text));
            card.addView(Ui.muted(this, n.date));
            card.setOnLongClickListener(v -> {
                p.notes.remove(n);
                touch(p, people);
                return true;
            });
            body.addView(card);
        }
        TextView addNote = Ui.button(this, "+ note");
        addNote.setOnClickListener(v -> addNote(p, people));
        body.addView(addNote);

        // --- promises ---
        body.addView(Ui.heading(this, "promises and follow-ups"));
        for (Model.Promise pr : p.promises) {
            LinearLayout card = Ui.card(this);
            CheckBox box = new CheckBox(this);
            box.setChecked(pr.done);
            box.setText(pr.text + (pr.due.isEmpty() ? "" : "  -  due " + pr.due));
            box.setTextSize(15);
            box.setOnCheckedChangeListener((btn, checked) -> {
                pr.done = checked;
                touch(p, people);
            });
            card.addView(box);
            card.setOnLongClickListener(v -> {
                p.promises.remove(pr);
                touch(p, people);
                return true;
            });
            body.addView(card);
        }
        TextView addPromise = Ui.button(this, "+ promise");
        addPromise.setOnClickListener(v -> addPromise(p, people));
        body.addView(addPromise);

        body.addView(Ui.muted(this, "long-press a card to remove it"));
    }

    private void touch(Model.Person p, List<Model.Person> people) {
        p.updated = System.currentTimeMillis();
        store.save(people);
        refresh();
    }

    // ---- contact picking: no READ_CONTACTS, one-off grant for the pick ----

    private void pickContact() {
        try {
            startActivityForResult(new Intent(Intent.ACTION_PICK,
                    ContactsContract.Contacts.CONTENT_URI), REQ_PICK_CONTACT);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "no contacts app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openContact(Model.Person p) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(p.contactUri)));
        } catch (Exception e) {
            // The picker's one-off grant can expire; the name stays linked.
            Toast.makeText(this,
                    "system no longer shares that contact with jumpa; find "
                            + p.contactName + " in Contacts, or unlink and pick again",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_CONTACT || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri == null) return;
        String displayName = null;
        try (Cursor c = getContentResolver().query(uri,
                new String[]{ContactsContract.Contacts.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) displayName = c.getString(0);
        } catch (Exception e) {
            displayName = null;
        }
        List<Model.Person> people = store.load();
        Model.Person p = person(people);
        if (p == null) return;
        p.contactUri = uri.toString();
        p.contactName = displayName != null ? displayName : "linked contact";
        touch(p, people);
    }

    // ---- editors ----

    private void addAffiliation(Model.Person p, List<Model.Person> people) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        form.setPadding(pad, pad, pad, pad);
        EditText org = new EditText(this);
        org.setHint("company or organization");
        EditText role = new EditText(this);
        role.setHint("role (optional)");
        EditText kind = new EditText(this);
        kind.setHint("kind: employer, client, community... (optional)");
        EditText start = new EditText(this);
        start.setHint("start, YYYY or YYYY-MM (optional)");
        EditText end = new EditText(this);
        end.setHint("end, YYYY or YYYY-MM (empty = current)");
        form.addView(org);
        form.addView(role);
        form.addView(kind);
        form.addView(start);
        form.addView(end);
        new AlertDialog.Builder(this)
                .setTitle("new affiliation")
                .setView(form)
                .setPositiveButton("add", (d, w) -> {
                    String orgText = org.getText().toString().trim();
                    String startText = start.getText().toString().trim();
                    String endText = end.getText().toString().trim();
                    if (orgText.isEmpty()) {
                        Toast.makeText(this, "name the company or organization", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!Model.dateOk(startText) || !Model.dateOk(endText)) {
                        Toast.makeText(this, "dates look like 2024 or 2024-06", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Model.Affiliation a = new Model.Affiliation();
                    a.org = orgText;
                    a.role = role.getText().toString().trim();
                    a.kind = kind.getText().toString().trim();
                    a.start = startText;
                    a.end = endText;
                    p.affiliations.add(a);
                    touch(p, people);
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    private void addNote(Model.Person p, List<Model.Person> people) {
        EditText input = new EditText(this);
        input.setHint("what should you remember?");
        input.setMinLines(3);
        int pad = Ui.dp(this, 16);
        input.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(this)
                .setTitle("new note")
                .setView(input)
                .setPositiveButton("add", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) return;
                    Model.Note n = new Model.Note();
                    n.text = text;
                    n.date = Model.today();
                    p.notes.add(n);
                    touch(p, people);
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    private void addPromise(Model.Person p, List<Model.Person> people) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 16);
        form.setPadding(pad, pad, pad, pad);
        EditText text = new EditText(this);
        text.setHint("what did you promise?");
        EditText due = new EditText(this);
        due.setHint("due, YYYY or YYYY-MM (optional)");
        form.addView(text);
        form.addView(due);
        new AlertDialog.Builder(this)
                .setTitle("new promise")
                .setView(form)
                .setPositiveButton("add", (d, w) -> {
                    String body = text.getText().toString().trim();
                    String dueText = due.getText().toString().trim();
                    if (body.isEmpty()) return;
                    if (!Model.dateOk(dueText)) {
                        Toast.makeText(this, "due looks like 2026 or 2026-12", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Model.Promise pr = new Model.Promise();
                    pr.text = body;
                    pr.due = dueText;
                    p.promises.add(pr);
                    touch(p, people);
                })
                .setNegativeButton("cancel", null)
                .show();
    }
}

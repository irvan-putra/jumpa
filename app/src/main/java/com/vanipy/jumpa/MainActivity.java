package com.vanipy.jumpa;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** People list. The whole screen is the list; controls live in the top row. */
public class MainActivity extends Activity {

    private Store store;
    private boolean showArchived;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);

        LinearLayout root = Ui.column(this);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView add = Ui.button(this, "+ person");
        add.setOnClickListener(v -> addPerson());
        TextView more = Ui.button(this, showArchived ? "active" : "archive");
        more.setOnClickListener(v -> {
            showArchived = !showArchived;
            more.setText(showArchived ? "active" : "archive");
            refresh();
        });
        TextView demo = Ui.button(this, store.demoMode() ? "demo: on" : "demo: off");
        demo.setOnClickListener(v -> {
            boolean on = !store.demoMode();
            store.setDemoMode(on);
            demo.setText(on ? "demo: on" : "demo: off");
            refresh();
        });
        View spacer = new View(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, 1, 1f);
        spacer.setLayoutParams(sp);
        top.addView(add);
        top.addView(spacer);
        top.addView(demo);
        top.addView(more);
        root.addView(top);

        list = Ui.column(this);
        list.setPadding(0, 0, 0, 0);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        list.removeAllViews();
        if (store.demoMode()) {
            TextView banner = Ui.muted(this, "demo people - fictional, nothing real stored");
            banner.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), 0, 0);
            list.addView(banner);
        }
        int shown = 0;
        List<Model.Person> people = store.load();
        for (Model.Person p : people) {
            if (p.archived != showArchived) continue;
            shown++;
            LinearLayout card = Ui.card(this);
            TextView name = Ui.line(this, p.name);
            card.addView(name);
            String sub = p.subtitle();
            if (!sub.isEmpty()) card.addView(Ui.muted(this, sub));
            int open = p.openPromises();
            StringBuilder meta = new StringBuilder();
            if (open > 0) meta.append(open).append(open == 1 ? " promise open" : " promises open");
            if (p.contactName != null) {
                if (meta.length() > 0) meta.append("  ·  ");
                meta.append("linked to ").append(p.contactName);
            }
            if (meta.length() > 0) card.addView(Ui.muted(this, meta.toString()));
            card.setOnClickListener(v -> {
                Intent i = new Intent(this, PersonActivity.class);
                i.putExtra("id", p.id);
                startActivity(i);
            });
            card.setOnLongClickListener(v -> {
                p.archived = !p.archived;
                p.updated = System.currentTimeMillis();
                store.save(people);
                Toast.makeText(this, p.archived ? "archived " + p.name : "restored " + p.name,
                        Toast.LENGTH_SHORT).show();
                refresh();
                return true;
            });
            list.addView(card);
        }
        if (shown == 0) {
            TextView empty = Ui.muted(this, showArchived
                    ? "no archived people"
                    : "no people yet - tap + person");
            empty.setPadding(Ui.dp(this, 4), Ui.dp(this, 24), 0, 0);
            list.addView(empty);
        }
    }

    private void addPerson() {
        EditText input = new EditText(this);
        input.setHint("name");
        int pad = Ui.dp(this, 16);
        input.setPadding(pad, pad, pad, pad);
        new AlertDialog.Builder(this)
                .setTitle("new person")
                .setView(input)
                .setPositiveButton("add", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    List<Model.Person> people = store.load();
                    Model.Person p = new Model.Person();
                    p.id = Model.newId();
                    p.name = name;
                    p.updated = System.currentTimeMillis();
                    people.add(p);
                    store.save(people);
                    refresh();
                })
                .setNegativeButton("cancel", null)
                .show();
    }
}

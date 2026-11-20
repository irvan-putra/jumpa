package com.vanipy.jumpa;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Version-one synthetic handoff. No network, contact grants or live inbox. */
public final class Encounter {
    public static final String ID = "kunjung-demo-design-2026-11";
    public static String validate(String name, String context, String date, String note) {
        if (name == null || name.trim().isEmpty() || name.length() > 120)
            return "name must be 1 to 120 characters";
        if (context == null || context.length() > 300 || note == null || note.length() > 2000)
            return "context or note is too long";
        if (date == null || date.length() > 10) return "use YYYY-MM-DD or leave the date empty";
        if (!date.isEmpty()) {
            try { LocalDate.parse(date); }
            catch (DateTimeParseException e) { return "use a real date, YYYY-MM-DD, or leave it empty"; }
        }
        return null;
    }
    public static boolean alreadySaved(List<Model.Person> people) {
        for (Model.Person p : people) if (p.encounterIds.contains(ID)) return true;
        return false;
    }
    public static String noteText(String context, boolean confirmed, String note) {
        String source = context.trim().isEmpty() ? "unknown" : context.trim();
        return "Kunjung synthetic encounter\n"
                + (confirmed ? "Where met (confirmed by you): " : "Arrival context (not proof of attendance): ")
                + source + (note.trim().isEmpty() ? "" : "\n" + note.trim());
    }
    private Encounter() {}
}

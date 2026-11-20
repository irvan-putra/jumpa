package com.vanipy.jumpa;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class EncounterTest {
    @Test public void validAndUnknownDate() {
        assertNull(Encounter.validate("Demo", "", "", ""));
        assertNull(Encounter.validate("Demo", "Invite", "2026-11-20", "Note"));
    }
    @Test public void invalidDates() {
        assertNotNull(Encounter.validate("Demo", "", "2026-02-30", ""));
        assertNotNull(Encounter.validate("Demo", "", "2026-11", ""));
    }
    @Test public void lengthLimits() {
        assertNotNull(Encounter.validate("", "", "", ""));
        assertNotNull(Encounter.validate("x".repeat(121), "", "", ""));
        assertNotNull(Encounter.validate("Demo", "x".repeat(301), "", ""));
        assertNotNull(Encounter.validate("Demo", "", "", "x".repeat(2001)));
    }
    @Test public void contextIsNeverAssumedAttendance() {
        assertTrue(Encounter.noteText("Invitation", false, "").contains("not proof of attendance"));
        assertFalse(Encounter.noteText("Invitation", false, "").contains("confirmed by you"));
    }
    @Test public void confirmationIsExplicit() {
        assertTrue(Encounter.noteText("Meetup", true, "Hello").contains("confirmed by you"));
        assertTrue(Encounter.noteText("Meetup", true, "Hello").endsWith("\nHello"));
    }
    @Test public void unknownContextStaysUnknown() {
        assertTrue(Encounter.noteText(" ", false, "").endsWith("unknown"));
    }
    @Test public void duplicateDetectionIncludesArchive() {
        List<Model.Person> people = new ArrayList<>();
        Model.Person p = new Model.Person();
        p.archived = true;
        people.add(p);
        assertFalse(Encounter.alreadySaved(people));
        p.encounterIds.add(Encounter.ID);
        assertTrue(Encounter.alreadySaved(people));
    }
    @Test public void namesNeverDeduplicatePeople() {
        List<Model.Person> people = new ArrayList<>();
        Model.Person p = new Model.Person();
        p.name = "Rani Demo";
        people.add(p);
        assertFalse(Encounter.alreadySaved(people));
    }
}

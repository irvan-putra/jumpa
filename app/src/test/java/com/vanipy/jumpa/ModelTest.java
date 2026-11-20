package com.vanipy.jumpa;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ModelTest {

    private Model.Affiliation aff(String org, String role, String start, String end) {
        Model.Affiliation a = new Model.Affiliation();
        a.org = org;
        a.role = role;
        a.start = start;
        a.end = end;
        return a;
    }

    @Test
    public void emptyEndMeansCurrent() {
        assertTrue(aff("PT Maju Bersama", "designer", "2024", "").isCurrent());
        assertFalse(aff("CV Kreatif", "junior", "2021", "2022").isCurrent());
    }

    @Test
    public void dateValidation() {
        assertTrue(Model.dateOk(""));
        assertTrue(Model.dateOk("2024"));
        assertTrue(Model.dateOk("2024-06"));
        assertFalse(Model.dateOk("2024-13"));
        assertFalse(Model.dateOk("June 2024"));
        assertFalse(Model.dateOk("2024-6"));
    }

    @Test
    public void rangeRendering() {
        assertEquals("2021-2022", aff("CV Kreatif", "junior", "2021", "2022").range());
        assertEquals("2024-", aff("PT Maju Bersama", "designer", "2024", "").range());
        assertEquals("", aff("PT Maju Bersama", "designer", "", "").range());
    }

    @Test
    public void descriptionFallsBackGracefully() {
        assertEquals("designer at PT Maju Bersama",
                aff("PT Maju Bersama", "designer", "2024", "").describe());
        assertEquals("Komunitas Sepeda", aff("Komunitas Sepeda", "", "2023", "").describe());
        assertEquals("coordinator", aff("", "coordinator", "2023", "").describe());
    }

    @Test
    public void currentAffiliationPicksNewestCurrentStart() {
        Model.Person p = new Model.Person();
        p.affiliations.add(aff("CV Kreatif", "junior", "2021", "2022"));
        p.affiliations.add(aff("PT Maju Bersama", "designer", "2024", ""));
        p.affiliations.add(aff("Komunitas Sepeda", "member", "2023", ""));
        Model.Affiliation current = p.currentAffiliation();
        assertEquals("PT Maju Bersama", current.org);
    }

    @Test
    public void noCurrentAffiliationGivesEmptySubtitle() {
        Model.Person p = new Model.Person();
        p.affiliations.add(aff("CV Kreatif", "junior", "2021", "2022"));
        assertNull(p.currentAffiliation());
        assertEquals("", p.subtitle());
    }

    @Test
    public void openPromiseCountIgnoresDone() {
        Model.Person p = new Model.Person();
        Model.Promise open = new Model.Promise();
        open.text = "send the checklist";
        Model.Promise done = new Model.Promise();
        done.text = "intro to committee";
        done.done = true;
        p.promises.add(open);
        p.promises.add(done);
        assertEquals(1, p.openPromises());
    }
}

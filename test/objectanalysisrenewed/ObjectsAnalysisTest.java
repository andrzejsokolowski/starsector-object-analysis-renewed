package objectanalysisrenewed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;

import org.junit.jupiter.api.Test;

class ObjectsAnalysisTest {

    @Test
    void detailDefaultsToPrecise() {
        assertEquals(Settings.Detail.PRECISE, Settings.parseDetail(null));
        assertEquals(Settings.Detail.PRECISE, Settings.parseDetail("Precise"));
        assertEquals(Settings.Detail.NORMAL, Settings.parseDetail("Normal"));
        assertEquals(Settings.Detail.VAGUE, Settings.parseDetail("Vague"));
        assertEquals(Settings.Detail.PRECISE, Settings.parseDetail("something else"));
    }

    @Test
    void shipLinesAreCountHullAndSize() {
        assertEquals("7x Muninn-class Frigate (Frigate)", ObjectsAnalysis.shipCountLabel(
                ObjectsAnalysis.hullLabel("Muninn-class Frigate", HullSize.FRIGATE), 7));
        assertEquals("2x Legion-class Carrier (Capital)", ObjectsAnalysis.shipCountLabel(
                ObjectsAnalysis.hullLabel("Legion-class Carrier", HullSize.CAPITAL_SHIP), 2));
        assertEquals("1x Muninn-class Fast Destroyer (Destroyer)", ObjectsAnalysis.shipCountLabel(
                ObjectsAnalysis.hullLabel("Muninn-class Fast Destroyer", HullSize.DESTROYER), 1));
        assertEquals("Odd Hull", ObjectsAnalysis.hullLabel("Odd Hull", HullSize.FIGHTER));
    }

    @Test
    void stanceLineListsOnlyKindsPresent() {
        assertEquals("Hostile fleet x3, Friendly fleet x2", ObjectsAnalysis.stanceLine(3, 0, 2));
        assertEquals("Hostile fleet x1, Neutral fleet x4, Friendly fleet x2", ObjectsAnalysis.stanceLine(1, 4, 2));
        assertEquals("Neutral fleet x1", ObjectsAnalysis.stanceLine(0, 1, 0));
    }

    @Test
    void repeatsAreCounted() {
        assertEquals("Debris Field x4", ObjectsAnalysis.countLabel("Debris Field", 4));
        assertEquals("Inactive Gate", ObjectsAnalysis.countLabel("Inactive Gate", 1));
    }

    @Test
    void scanCostCommodity() {
        assertEquals("volatiles", Settings.costCommodity("Volatiles"));
        assertEquals("rare_metals", Settings.costCommodity("Transplutonics"));
        assertNull(Settings.costCommodity("Nothing"));
        assertEquals("volatiles", Settings.costCommodity(null));
    }

    @Test
    void scanCostText() {
        assertEquals("5 volatiles", new Settings.Snapshot(Settings.Detail.PRECISE, true, true, true, "Volatiles", 5).costText());
        assertEquals("3 transplutonics", new Settings.Snapshot(Settings.Detail.PRECISE, true, true, true, "Transplutonics", 3).costText());
        assertEquals("nothing", new Settings.Snapshot(Settings.Detail.PRECISE, true, true, true, "Nothing", 5).costText());
        assertEquals("nothing", new Settings.Snapshot(Settings.Detail.PRECISE, true, true, true, "Volatiles", 0).costText());
    }

    @Test
    void surveyDefaultsToPreliminary() {
        assertEquals(Settings.Survey.PRELIMINARY, Settings.parseSurvey(null));
        assertEquals(Settings.Survey.PRELIMINARY, Settings.parseSurvey("Preliminary"));
        assertEquals(Settings.Survey.FULL, Settings.parseSurvey("Full"));
        assertEquals(Settings.Survey.NONE, Settings.parseSurvey("None"));
    }

    @Test
    void scanMessageNamesWhatHappened() {
        assertEquals("Objects Analysis: the scan found nothing new.", ObjectsAnalysis.scanMessage(0, 0));
        assertEquals("Objects Analysis: the scan found 3 new objects and surveyed 1 planet.", ObjectsAnalysis.scanMessage(3, 1));
        assertEquals("Objects Analysis: the scan surveyed 4 planets.", ObjectsAnalysis.scanMessage(0, 4));
        assertEquals("Objects Analysis: the scan found 1 new object.", ObjectsAnalysis.scanMessage(1, 0));
    }

    @Test
    void hiddenListsHoldWhatAndrzejRuledOut() {
        assertTrue(ObjectsAnalysis.HIDDEN_TYPES.contains("stellar_mirror"));
        assertTrue(ObjectsAnalysis.HIDDEN_TYPES.contains("fusion_lamp"));
        assertTrue(ObjectsAnalysis.HIDDEN_TAGS.contains("niko_MPC_satellite"));
        assertFalse(ObjectsAnalysis.HIDDEN_TYPES.contains("inactive_gate"));
    }

    @Test
    void scanLineAddsSurveyOnlyWhenOn() {
        String base = "Active Ability: System-wide scan: for 5 seconds, prepare a scan pulse that reveals and "
                + "discovers all objects in the system";
        assertEquals(base, ObjectsAnalysis.scanLine(Settings.Survey.NONE));
        assertEquals(base + ", which also performs a preliminary survey of all planets",
                ObjectsAnalysis.scanLine(Settings.Survey.PRELIMINARY));
        assertEquals(base + ", which also performs a full survey of all planets",
                ObjectsAnalysis.scanLine(Settings.Survey.FULL));
        assertEquals("Lists all objects in the current star system.", ObjectsAnalysis.INTRO);
    }
}

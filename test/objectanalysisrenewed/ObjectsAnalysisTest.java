package objectanalysisrenewed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void shipLabelHasNameAndClass() {
        assertEquals("ISS Hamatsu, Legion-class Carrier", ObjectsAnalysis.shipLabel("ISS Hamatsu", "Legion-class Carrier"));
        assertEquals("Legion-class Carrier", ObjectsAnalysis.shipLabel(null, "Legion-class Carrier"));
        assertEquals("Legion-class Carrier", ObjectsAnalysis.shipLabel("  ", "Legion-class Carrier"));
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
    void hiddenListsHoldWhatAndrzejRuledOut() {
        assertTrue(ObjectsAnalysis.HIDDEN_TYPES.contains("stellar_mirror"));
        assertTrue(ObjectsAnalysis.HIDDEN_TYPES.contains("fusion_lamp"));
        assertTrue(ObjectsAnalysis.HIDDEN_TAGS.contains("niko_MPC_satellite"));
        assertFalse(ObjectsAnalysis.HIDDEN_TYPES.contains("inactive_gate"));
    }
}

package objectanalysisrenewed;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The rule for when the list appears must match Harpuea's Objects Analysis 002 (the shipped jar). */
class ObjectsAnalysisTest {

    private static final float GOOD_ROLL = 0.9f;
    private static final float BAD_ROLL = 0.1f;

    @Test
    void stationOrNeutrinoCounts() {
        assertTrue(ObjectsAnalysis.counts(false, true, false, false, BAD_ROLL));
        assertTrue(ObjectsAnalysis.counts(false, false, false, true, BAD_ROLL));
    }

    @Test
    void untaggedNeverCounts() {
        assertFalse(ObjectsAnalysis.counts(false, false, false, false, GOOD_ROLL));
    }

    @Test
    void strongSourceIsSkipped() {
        assertFalse(ObjectsAnalysis.counts(true, true, false, true, GOOD_ROLL));
    }

    @Test
    void faintSourceCountsOnAGoodRollOnly() {
        assertTrue(ObjectsAnalysis.counts(false, false, true, false, GOOD_ROLL));
        assertFalse(ObjectsAnalysis.counts(false, false, true, false, BAD_ROLL));
        // As in the original: a faint tag is skipped on a bad roll even with another tag.
        assertFalse(ObjectsAnalysis.counts(false, false, true, true, BAD_ROLL));
        assertTrue(ObjectsAnalysis.counts(false, false, true, true, GOOD_ROLL));
    }

    @Test
    void detailDefaultsToPrecise() {
        org.junit.jupiter.api.Assertions.assertEquals(Settings.Detail.PRECISE, Settings.parseDetail(null));
        org.junit.jupiter.api.Assertions.assertEquals(Settings.Detail.PRECISE, Settings.parseDetail("Precise"));
        org.junit.jupiter.api.Assertions.assertEquals(Settings.Detail.NORMAL, Settings.parseDetail("Normal"));
        org.junit.jupiter.api.Assertions.assertEquals(Settings.Detail.VAGUE, Settings.parseDetail("Vague"));
        org.junit.jupiter.api.Assertions.assertEquals(Settings.Detail.PRECISE, Settings.parseDetail("something else"));
    }

    @Test
    void shipLabelHasNameAndClass() {
        org.junit.jupiter.api.Assertions.assertEquals("ISS Hamatsu, Legion-class Carrier",
                ObjectsAnalysis.shipLabel("ISS Hamatsu", "Legion-class Carrier"));
        org.junit.jupiter.api.Assertions.assertEquals("Legion-class Carrier",
                ObjectsAnalysis.shipLabel(null, "Legion-class Carrier"));
        org.junit.jupiter.api.Assertions.assertEquals("Legion-class Carrier",
                ObjectsAnalysis.shipLabel("  ", "Legion-class Carrier"));
    }

    @Test
    void stanceLineListsOnlyKindsPresent() {
        org.junit.jupiter.api.Assertions.assertEquals("Hostile fleet x3, Friendly fleet x2",
                ObjectsAnalysis.stanceLine(3, 0, 2));
        org.junit.jupiter.api.Assertions.assertEquals("Hostile fleet x1, Neutral fleet x4, Friendly fleet x2",
                ObjectsAnalysis.stanceLine(1, 4, 2));
        org.junit.jupiter.api.Assertions.assertEquals("Neutral fleet x1", ObjectsAnalysis.stanceLine(0, 1, 0));
    }

    @Test
    void skipChanceIsTheShippedJarsValue() {
        assertTrue(ObjectsAnalysis.counts(false, false, true, false, 0.3f));
        assertFalse(ObjectsAnalysis.counts(false, false, true, false, 0.2999f));
    }
}

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
    void skipChanceIsTheShippedJarsValue() {
        assertTrue(ObjectsAnalysis.counts(false, false, true, false, 0.3f));
        assertFalse(ObjectsAnalysis.counts(false, false, true, false, 0.2999f));
    }
}

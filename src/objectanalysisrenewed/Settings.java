package objectanalysisrenewed;

import com.fs.starfarer.api.Global;

/**
 * The LunaLib settings. LunaLib is optional: without it the defaults apply. Only {@link LunaReader} names
 * LunaLib's classes, so nothing loads them when LunaLib is off.
 *
 * The ability bar asks isUsable() every frame, so {@link #now()} hands out a snapshot read at most once a
 * second of real time. The tooltip uses {@link #fresh()}.
 */
public final class Settings {

    static final String MOD_ID = "object_analysis_renewed";
    static final String LUNALIB = "lunalib";

    /** How much the list says about each object. */
    public enum Detail {
        /** Derelict ships counted by hull and size ("7x Muninn-class Frigate (Frigate)"), fleets by name. */
        PRECISE,
        /** Object names as the game shows them, fleets counted by hostility. */
        NORMAL,
        /** Only whether something is left to salvage, fleets as one count. */
        VAGUE
    }

    /** How far the active scan surveys the planets in the system. */
    public enum Survey { PRELIMINARY, FULL, NONE }

    static final String DETAIL_PRECISE = "Precise";
    static final String DETAIL_NORMAL = "Normal";
    static final String DETAIL_VAGUE = "Vague";

    static final String COST_VOLATILES = "Volatiles";
    static final String COST_TRANSPLUTONICS = "Transplutonics";
    static final String COST_NOTHING = "Nothing";
    static final int DEFAULT_COST_AMOUNT = 5;

    private static final long REFRESH_NANOS = 1_000_000_000L;
    private static Snapshot cached;
    private static long cachedAt;

    private Settings() {}

    /** Everything the ability reads, at one moment. */
    public static final class Snapshot {
        final Detail detail;
        final boolean showFleets;
        final boolean greyFound;
        final boolean activeScan;
        final String cost;
        final int costAmount;
        final Survey survey;
        /** The scan halves the burn level instead of stopping the fleet, and Sustained Burn stays on (OAR-19). */
        final boolean keepBurn;

        Snapshot(Detail detail, boolean showFleets, boolean greyFound, boolean activeScan, String cost, int costAmount) {
            this(detail, showFleets, greyFound, activeScan, cost, costAmount, Survey.PRELIMINARY);
        }

        Snapshot(Detail detail, boolean showFleets, boolean greyFound, boolean activeScan, String cost, int costAmount,
                Survey survey) {
            this(detail, showFleets, greyFound, activeScan, cost, costAmount, survey, true);
        }

        Snapshot(Detail detail, boolean showFleets, boolean greyFound, boolean activeScan, String cost, int costAmount,
                Survey survey, boolean keepBurn) {
            this.detail = detail;
            this.showFleets = showFleets;
            this.greyFound = greyFound;
            this.activeScan = activeScan;
            this.cost = cost;
            this.costAmount = Math.max(0, costAmount);
            this.survey = survey == null ? Survey.PRELIMINARY : survey;
            this.keepBurn = keepBurn;
        }

        /** The commodity id the scan uses up, or null when it is free. */
        String costCommodity() {
            return Settings.costCommodity(cost);
        }

        /** "5 volatiles", or "nothing". */
        String costText() {
            String c = costCommodity();
            if (c == null || costAmount <= 0) return "nothing";
            return costAmount + (COST_TRANSPLUTONICS.equalsIgnoreCase(cost) ? " transplutonics" : " volatiles");
        }
    }

    static String costCommodity(String cost) {
        if (COST_TRANSPLUTONICS.equalsIgnoreCase(cost)) return "rare_metals";
        if (COST_NOTHING.equalsIgnoreCase(cost)) return null;
        return "volatiles";
    }

    /** A snapshot at most a second old. */
    public static Snapshot now() {
        long t = System.nanoTime();
        Snapshot s = cached;
        if (s == null || t - cachedAt >= REFRESH_NANOS) {
            s = read();
            cached = s;
            cachedAt = t;
        }
        return s;
    }

    /** A snapshot read right now (the tooltip, where a changed setting should show at once). */
    public static Snapshot fresh() {
        Snapshot s = read();
        cached = s;
        cachedAt = System.nanoTime();
        return s;
    }

    private static Snapshot read() {
        Boolean fleets = bool("oar_fleets");
        Boolean grey = bool("oar_greyFound");
        Boolean active = bool("oar_activeScan");
        Integer amount = integer("oar_costAmount");
        Boolean keepBurn = bool("oar_keepBurn");
        return new Snapshot(parseDetail(string("oar_detail")),
                fleets == null || fleets,
                grey == null || grey,
                active == null || active,
                string("oar_cost"),
                amount == null ? DEFAULT_COST_AMOUNT : amount,
                parseSurvey(string("oar_survey")),
                keepBurn == null || keepBurn);
    }

    /** Unknown or missing text gives the default, Preliminary. */
    static Survey parseSurvey(String s) {
        if ("Full".equalsIgnoreCase(s)) return Survey.FULL;
        if ("None".equalsIgnoreCase(s)) return Survey.NONE;
        return Survey.PRELIMINARY;
    }

    /** Unknown or missing text gives the default, Precise. */
    static Detail parseDetail(String s) {
        if (DETAIL_NORMAL.equalsIgnoreCase(s)) return Detail.NORMAL;
        if (DETAIL_VAGUE.equalsIgnoreCase(s)) return Detail.VAGUE;
        return Detail.PRECISE;
    }

    private static boolean luna() {
        try {
            return Global.getSettings().getModManager().isModEnabled(LUNALIB);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String string(String id) {
        if (!luna()) return null;
        try {
            return LunaReader.string(MOD_ID, id);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static Boolean bool(String id) {
        if (!luna()) return null;
        try {
            return LunaReader.bool(MOD_ID, id);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private static Integer integer(String id) {
        if (!luna()) return null;
        try {
            return LunaReader.integer(MOD_ID, id);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }
}

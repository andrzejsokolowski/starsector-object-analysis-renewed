package objectanalysisrenewed;

import com.fs.starfarer.api.Global;

/**
 * The LunaLib settings, read when the tooltip opens. LunaLib is optional: without it the defaults
 * apply. Only {@link LunaReader} names LunaLib's classes, so nothing loads them when LunaLib is off.
 */
public final class Settings {

    static final String MOD_ID = "object_analysis_renewed";
    static final String LUNALIB = "lunalib";

    /** How much the list says about each object. */
    public enum Detail {
        /** Real names: derelict ships by ship name and class, fleets by name. */
        PRECISE,
        /** Object names as the game shows them, fleets counted by hostility. */
        NORMAL,
        /** Only whether something is left to salvage, fleets as one count. */
        VAGUE
    }

    static final String DETAIL_PRECISE = "Precise";
    static final String DETAIL_NORMAL = "Normal";
    static final String DETAIL_VAGUE = "Vague";

    private Settings() {}

    public static Detail detail() {
        return parseDetail(string("oar_detail"));
    }

    public static boolean showFleets() {
        Boolean b = bool("oar_fleets");
        return b == null || b;
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
}

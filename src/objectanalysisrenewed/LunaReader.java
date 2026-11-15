package objectanalysisrenewed;

import lunalib.lunaSettings.LunaSettings;

/** The only class that names LunaLib. Called only while LunaLib is enabled. */
final class LunaReader {

    private LunaReader() {}

    static String string(String modId, String id) {
        return LunaSettings.getString(modId, id);
    }

    static Boolean bool(String modId, String id) {
        return LunaSettings.getBoolean(modId, id);
    }

    static Integer integer(String modId, String id) {
        return LunaSettings.getInt(modId, id);
    }
}

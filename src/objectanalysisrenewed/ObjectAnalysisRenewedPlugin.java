package objectanalysisrenewed;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

/**
 * Gives the player the ability on every load, and takes it off for the length of each save so a save
 * never depends on this mod. Same as the original's plugin.
 */
public class ObjectAnalysisRenewedPlugin extends BaseModPlugin {

    /** The original's ability id, kept so the ability bar slot and old saves carry over. */
    public static final String ABILITY_ID = "objectsanalysis";

    /** Harpuea's original. */
    static final String ORIGINAL_ID = "objects_analysis";

    /**
     * Both mods define the same ability, and the original loads after this one (its folder sorts later), so
     * with both on the slow version would win. Stop at start with a clear message instead.
     */
    @Override
    public void onApplicationLoad() {
        boolean original;
        try {
            original = Global.getSettings().getModManager().isModEnabled(ORIGINAL_ID);
        } catch (RuntimeException e) {
            return;
        }
        if (original) {
            throw new RuntimeException("Object Analysis - Renewed replaces Objects Analysis. "
                    + "Turn Objects Analysis off in the launcher or your mod manager, then start the game again.");
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        try {
            CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
            if (fleet == null || !fleet.hasAbility(ABILITY_ID)) {
                Global.getSector().getCharacterData().addAbility(ABILITY_ID);
            }
        } catch (RuntimeException e) {
            Global.getLogger(ObjectAnalysisRenewedPlugin.class).error("Object Analysis - Renewed: could not add the ability", e);
        }
    }

    @Override
    public void beforeGameSave() {
        try {
            Global.getSector().getCharacterData().removeAbility(ABILITY_ID);
        } catch (RuntimeException e) {
            Global.getLogger(ObjectAnalysisRenewedPlugin.class).error("Object Analysis - Renewed: could not take the ability off before the save", e);
        }
    }

    @Override
    public void afterGameSave() {
        try {
            Global.getSector().getCharacterData().addAbility(ABILITY_ID);
        } catch (RuntimeException e) {
            Global.getLogger(ObjectAnalysisRenewedPlugin.class).error("Object Analysis - Renewed: could not put the ability back after the save", e);
        }
    }

    @Override
    public void onGameSaveFailed() {
        afterGameSave();
    }
}

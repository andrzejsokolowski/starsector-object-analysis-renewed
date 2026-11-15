package objectanalysisrenewed;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Objects Analysis, as in Harpuea's Objects Analysis 002: an ability that is never switched on. Its
 * tooltip lists the objects in the current star system that a neutrino or gravitic scan could pick up,
 * without saying where they are.
 *
 * The original worked out that list in isUsable(), which the ability bar calls every frame, only to
 * return false anyway. Each time it walked every custom entity in the system and, for each one with a
 * neutrino or station tag, searched the whole system four more times. With many objects that cost about
 * 7% of every campaign frame. Here isUsable() answers at once, and the list is built only when the
 * tooltip is made, with each tag searched once.
 */
public class ObjectsAnalysis extends BaseDurationAbility {

    public static final float SURVEY_RANGE = 500f;
    public static final float DETECTABILITY_RANGE_BONUS = 2000f;
    public static final float ACCELERATION_MULT = 4f;
    /** The shipped 002 jar skips a faint ("neutrino_low") object 30% of the time. */
    static final float NEUTRINO_LOW_SKIP_PROB = 0.3f;

    static final String NEUTRINO = "neutrino";
    static final String NEUTRINO_LOW = "neutrino_low";
    static final String NEUTRINO_HIGH = "neutrino_high";
    static final String STATION = "station";

    protected boolean performed = false;

    @Override
    protected void activateImpl() {
        if (entity.isInCurrentLocation()) {
            SectorEntityToken.VisibilityLevel level = entity.getVisibilityLevelToPlayerFleet();
            if (level != SectorEntityToken.VisibilityLevel.NONE) {
                Global.getSector().addPing(entity, "remote_survey");
            }
            performed = false;
        }
    }

    @Override
    protected void applyEffect(float amount, float level) {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return;
        fleet.getStats().getFleetwideMaxBurnMod().modifyMult(getModId(), 0f, "ObjectsAnalysis");
        fleet.getStats().getDetectedRangeMod().modifyFlat(getModId(), DETECTABILITY_RANGE_BONUS * level, "ObjectsAnalysis");
        fleet.getStats().getAccelerationMult().modifyMult(getModId(), ACCELERATION_MULT * level);
        if (!performed && level >= 1f) performed = true;
    }

    /** It never needs switching on, so it is never usable. Called every frame: no work here. */
    @Override
    public boolean isUsable() {
        return false;
    }

    /**
     * The objects to list: when the system holds at least one custom entity that counts (a station or
     * neutrino source, a faint source only 70% of the time, never a strong one on its own), every entity
     * tagged neutrino, faint, strong or station, each once. The same list the original built.
     */
    protected List<SectorEntityToken> getSurveyableInRange() {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return new ArrayList<>();
        LocationAPI location = fleet.getContainingLocation();
        if (location == null) return new ArrayList<>();

        boolean any = false;
        for (Object o : location.getEntities(CustomCampaignEntityAPI.class)) {
            if (!(o instanceof SectorEntityToken)) continue;
            SectorEntityToken e = (SectorEntityToken) o;
            if (counts(e.hasTag(NEUTRINO_HIGH), e.hasTag(NEUTRINO), e.hasTag(NEUTRINO_LOW), e.hasTag(STATION),
                    (float) Math.random())) {
                any = true;
                break;
            }
        }
        if (!any) return new ArrayList<>();

        Set<SectorEntityToken> found = new LinkedHashSet<>();
        found.addAll(location.getEntitiesWithTag(NEUTRINO));
        found.addAll(location.getEntitiesWithTag(NEUTRINO_LOW));
        found.addAll(location.getEntitiesWithTag(NEUTRINO_HIGH));
        found.addAll(location.getEntitiesWithTag(STATION));
        return new ArrayList<>(found);
    }

    /** Whether one custom entity makes the list appear, as the original decided it. */
    static boolean counts(boolean high, boolean neutrino, boolean low, boolean station, float roll) {
        if (high) return false;
        if (!neutrino && !low && !station) return false;
        // Anything tagged faint is skipped on a bad roll, as in the original.
        return !(low && roll < NEUTRINO_LOW_SKIP_PROB);
    }

    @Override
    protected void deactivateImpl() {
        cleanupImpl();
    }

    @Override
    protected void cleanupImpl() {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return;
        fleet.getStats().getDetectedRangeMod().unmodify(getModId());
        fleet.getStats().getFleetwideMaxBurnMod().unmodify(getModId());
        fleet.getStats().getAccelerationMult().unmodify(getModId());
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return;
        Color gray = Misc.getGrayColor();
        Color highlight = Misc.getHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();
        float pad = 10f;

        tooltip.addTitle(spec.getName());
        tooltip.addPara("Coordinate the fleet's active sensor network to perform a scan of all objects within current sector.",
                pad, highlight, String.valueOf(SURVEY_RANGE));

        List<SectorEntityToken> objects = getSurveyableInRange();
        if (objects.isEmpty()) {
            tooltip.addPara("No objects in range.", bad, pad);
        } else {
            tooltip.addPara("The following objects are in range:", pad);
            float currPad = 3f;
            for (SectorEntityToken object : objects) {
                tooltip.addPara("    %s", currPad, object.getIndicatorColor(), object.getFullName());
                currPad = 0f;
            }
        }
        tooltip.addPara("*2000 units = 1 map grid cell", gray, pad);
        addIncompatibleToTooltip(tooltip, expanded);
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }
}

package objectanalysisrenewed;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
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

        Settings.Detail detail = Settings.detail();
        List<SectorEntityToken> objects = getSurveyableInRange();
        if (detail == Settings.Detail.VAGUE) {
            boolean salvage = false;
            for (SectorEntityToken object : objects) {
                if (salvageable(object)) {
                    salvage = true;
                    break;
                }
            }
            if (salvage) tooltip.addPara(VAGUE_SALVAGE, pad, highlight, VAGUE_SALVAGE);
            else tooltip.addPara(VAGUE_NONE, bad, pad);
        } else if (objects.isEmpty()) {
            tooltip.addPara("No objects in range.", bad, pad);
        } else {
            tooltip.addPara("The following objects are in range:", pad);
            float currPad = 3f;
            for (SectorEntityToken object : objects) {
                String name = detail == Settings.Detail.PRECISE ? preciseName(object) : object.getFullName();
                tooltip.addPara("    %s", currPad, object.getIndicatorColor(), name);
                currPad = 0f;
            }
        }

        if (Settings.showFleets()) addFleets(tooltip, fleet, detail, pad, bad);
        tooltip.addPara("*2000 units = 1 map grid cell", gray, pad);
        addIncompatibleToTooltip(tooltip, expanded);
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    // ---- Detail levels ----

    static final String VAGUE_SALVAGE = "Something here can still be salvaged.";
    static final String VAGUE_NONE = "Nothing here is left to salvage.";
    /** In hyperspace, fleets count only this close (one map grid cell), not across the whole of it. */
    static final float HYPERSPACE_FLEET_RANGE = 2000f;

    /** A derelict ship by its name and class ("ISS Hamatsu, Legion-class Carrier"); anything else as the game names it. */
    static String preciseName(SectorEntityToken object) {
        if (object.getCustomPlugin() instanceof DerelictShipEntityPlugin) {
            DerelictShipEntityPlugin plugin = (DerelictShipEntityPlugin) object.getCustomPlugin();
            DerelictShipEntityPlugin.DerelictShipData data = plugin.getData();
            ShipVariantAPI variant = data == null || data.ship == null ? null : data.ship.getVariant();
            if (variant != null && variant.getHullSpec() != null) {
                return shipLabel(data.ship.shipName, variant.getHullSpec().getNameWithDesignationWithDashClass());
            }
        }
        return object.getFullName();
    }

    static String shipLabel(String shipName, String hullClass) {
        if (shipName == null || shipName.trim().isEmpty()) return hullClass;
        return shipName + ", " + hullClass;
    }

    /** Anything with loot still in it: salvage targets and cargo pods. */
    static boolean salvageable(SectorEntityToken object) {
        if (object.hasTag(Tags.SALVAGEABLE)) return true;
        String type = object.getCustomEntityType();
        return type != null && type.startsWith("cargo_pod");
    }

    /** Fleets in the system: every one but the player's and stations; in hyperspace, only nearby ones. */
    static List<CampaignFleetAPI> fleetsInRange(CampaignFleetAPI player) {
        List<CampaignFleetAPI> out = new ArrayList<>();
        LocationAPI location = player.getContainingLocation();
        if (location == null) return out;
        boolean hyper = location.isHyperspace();
        for (CampaignFleetAPI f : location.getFleets()) {
            if (f == player || f.isStationMode() || f.isPlayerFleet()) continue;
            if (hyper && Misc.getDistance(f, player) > HYPERSPACE_FLEET_RANGE) continue;
            out.add(f);
        }
        return out;
    }

    private void addFleets(TooltipMakerAPI tooltip, CampaignFleetAPI player, Settings.Detail detail, float pad, Color bad) {
        List<CampaignFleetAPI> fleets = fleetsInRange(player);
        if (fleets.isEmpty()) {
            tooltip.addPara("No fleets in range.", bad, pad);
            return;
        }
        if (detail == Settings.Detail.VAGUE) {
            String line = "Fleet x" + fleets.size();
            tooltip.addPara(line, pad, Misc.getHighlightColor(), line);
            return;
        }
        if (detail == Settings.Detail.NORMAL) {
            int hostile = 0, neutral = 0, friendly = 0;
            for (CampaignFleetAPI f : fleets) {
                switch (stance(f, player)) {
                    case HOSTILE:
                        hostile++;
                        break;
                    case FRIENDLY:
                        friendly++;
                        break;
                    default:
                        neutral++;
                }
            }
            tooltip.addPara(stanceLine(hostile, neutral, friendly), pad);
            return;
        }
        // Precise: every fleet by name, the same names counted together.
        tooltip.addPara("Fleets in range:", pad);
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, Color> colors = new LinkedHashMap<>();
        for (CampaignFleetAPI f : fleets) {
            String name = f.getFullName();
            counts.merge(name, 1, Integer::sum);
            colors.putIfAbsent(name, f.getIndicatorColor());
        }
        float currPad = 3f;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            String label = e.getValue() > 1 ? e.getKey() + " x" + e.getValue() : e.getKey();
            tooltip.addPara("    %s", currPad, colors.get(e.getKey()), label);
            currPad = 0f;
        }
    }

    enum Stance { HOSTILE, NEUTRAL, FRIENDLY }

    private static Stance stance(CampaignFleetAPI f, CampaignFleetAPI player) {
        if (f.isHostileTo(player)) return Stance.HOSTILE;
        if (f.getFaction() == null) return Stance.NEUTRAL;
        if (f.getFaction().isPlayerFaction()) return Stance.FRIENDLY;
        return f.getFaction().getRelationshipLevel(Factions.PLAYER).isAtWorst(RepLevel.FAVORABLE)
                ? Stance.FRIENDLY : Stance.NEUTRAL;
    }

    /** "Hostile fleet x3, Friendly fleet x2": only the kinds there are. */
    static String stanceLine(int hostile, int neutral, int friendly) {
        List<String> parts = new ArrayList<>();
        if (hostile > 0) parts.add("Hostile fleet x" + hostile);
        if (neutral > 0) parts.add("Neutral fleet x" + neutral);
        if (friendly > 0) parts.add("Friendly fleet x" + friendly);
        return String.join(", ", parts);
    }
}

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
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.DiscoverEntityPlugin;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.DerelictShipEntityPlugin;
import com.fs.starfarer.api.impl.campaign.abilities.BaseDurationAbility;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/**
 * Objects Analysis, remade from Harpuea's Objects Analysis 002.
 *
 * Passive: hovering the ability lists the objects in the current star system (stations, gates, derelicts,
 * caches and other neutrino sources), without saying where they are. The original built that list in
 * isUsable(), which the ability bar calls every frame; here isUsable() is cheap and the list is built only
 * when the tooltip opens.
 *
 * Active scan (setting, on by default): clicking the ability costs some cargo, stops the fleet for about
 * five seconds and then discovers every object in the system, the same way flying up to it would (named
 * on the map, the "Discovered" message, discovery experience).
 *
 * Works only inside star systems, never in hyperspace.
 */
public class ObjectsAnalysis extends BaseDurationAbility {

    /** While scanning the fleet is easier to see from afar. */
    public static final float DETECTABILITY_RANGE_BONUS = 2000f;

    static final String NEUTRINO = "neutrino";
    static final String NEUTRINO_LOW = "neutrino_low";
    static final String NEUTRINO_HIGH = "neutrino_high";
    static final String STATION = "station";

    /** Never listed: colony structures and clutter Andrzej does not want (OAR-3). */
    static final Set<String> HIDDEN_TYPES = new LinkedHashSet<>(java.util.Arrays.asList(
            "stellar_mirror", "stellar_shade", "fusion_lamp"));
    /** What We Left Behind's anti-asteroid satellites: up to about 20 around one planet. */
    static final Set<String> HIDDEN_TAGS = new LinkedHashSet<>(java.util.Arrays.asList(
            "niko_MPC_satellite"));

    protected boolean performed = false;

    // ---- Active scan ----

    @Override
    protected void activateImpl() {
        performed = false;
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null || !fleet.isPlayerFleet()) return;
        Settings.Snapshot s = Settings.now();
        String commodity = s.costCommodity();
        if (commodity != null && s.costAmount > 0) {
            fleet.getCargo().removeCommodity(commodity, s.costAmount);
        }
        if (entity.isInCurrentLocation()) Global.getSector().addPing(entity, "remote_survey");
    }

    @Override
    protected void applyEffect(float amount, float level) {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return;
        // Full stop while the pulse charges, and easier to spot.
        fleet.getStats().getFleetwideMaxBurnMod().modifyMult(getModId(), 0f, "Objects Analysis scan");
        fleet.getStats().getDetectedRangeMod().modifyFlat(getModId(), DETECTABILITY_RANGE_BONUS * level, "Objects Analysis scan");
        if (!performed && level >= 1f) {
            performed = true;
            int found = discoverAll(fleet.getContainingLocation());
            if (fleet.isPlayerFleet()) {
                Global.getSector().getCampaignUI().addMessage(found > 0
                        ? "Objects Analysis: the scan found " + found + (found == 1 ? " new object." : " new objects.")
                        : "Objects Analysis: the scan found nothing new.",
                        found > 0 ? Misc.getPositiveHighlightColor() : Misc.getGrayColor());
            }
        }
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
    }

    /**
     * Called every frame by the ability bar. Settings come from a snapshot read at most once a second,
     * so this stays a few field reads.
     */
    @Override
    public boolean isUsable() {
        Settings.Snapshot s = Settings.now();
        if (!s.activeScan) return false;
        if (!super.isUsable()) return false;
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null || fleet.isInHyperspace() || fleet.isInHyperspaceTransition()) return false;
        return canPay(fleet, s);
    }

    private static boolean canPay(CampaignFleetAPI fleet, Settings.Snapshot s) {
        String commodity = s.costCommodity();
        if (commodity == null || s.costAmount <= 0) return true;
        CargoAPI cargo = fleet.getCargo();
        return cargo != null && cargo.getCommodityQuantity(commodity) >= s.costAmount;
    }

    /**
     * Every object in the location that is still waiting to be discovered, discovered through the game's
     * own discovery plugin (map name, message, experience, salvor's tally). Returns how many.
     */
    static int discoverAll(LocationAPI location) {
        if (location == null || location.isHyperspace()) return 0;
        int n = 0;
        for (SectorEntityToken e : new ArrayList<>(location.getAllEntities())) {
            if (!e.isDiscoverable()) continue;
            DiscoverEntityPlugin plugin = null;
            try {
                Object picked = Global.getSector().getGenericPlugins().pickPlugin(DiscoverEntityPlugin.class, e);
                if (picked instanceof DiscoverEntityPlugin) plugin = (DiscoverEntityPlugin) picked;
            } catch (RuntimeException ignored) {
                // Fall back below.
            }
            if (plugin != null) {
                plugin.discoverEntity(e);
            } else {
                e.setDiscoverable(null);
                e.setSensorProfile(null);
            }
            n++;
        }
        return n;
    }

    // ---- The list ----

    /** Every entity tagged neutrino (any strength) or station in the location, minus the hidden ones, each once. */
    protected List<SectorEntityToken> getSurveyableInRange() {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return new ArrayList<>();
        LocationAPI location = fleet.getContainingLocation();
        if (location == null || location.isHyperspace()) return new ArrayList<>();
        Set<SectorEntityToken> found = new LinkedHashSet<>();
        for (String tag : new String[] {NEUTRINO, NEUTRINO_LOW, NEUTRINO_HIGH, STATION}) {
            for (SectorEntityToken e : location.getEntitiesWithTag(tag)) {
                if (!hidden(e)) found.add(e);
            }
        }
        return new ArrayList<>(found);
    }

    /** Stellar mirrors and shades, fusion lamps, WWLB satellites, and every station that belongs to a colony. */
    static boolean hidden(SectorEntityToken e) {
        String type = e.getCustomEntityType();
        if (type != null && HIDDEN_TYPES.contains(type)) return true;
        for (String tag : HIDDEN_TAGS) if (e.hasTag(tag)) return true;
        return e.hasTag(STATION) && colony(e.getMarket());
    }

    /** A real colony's market, not the condition-only market an empty planet or derelict station carries. */
    static boolean colony(MarketAPI market) {
        return market != null && !market.isPlanetConditionMarketOnly();
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return;
        Settings.Snapshot s = Settings.fresh();
        Color gray = Misc.getGrayColor();
        Color highlight = Misc.getHighlightColor();
        Color bad = Misc.getNegativeHighlightColor();
        float pad = 10f;

        tooltip.addTitle(spec.getName());
        tooltip.addPara("Lists the objects in the current star system that your sensors can pick up, without "
                + "their locations.", pad);
        if (fleet.isInHyperspace() || fleet.isInHyperspaceTransition()) {
            tooltip.addPara("Works only inside a star system.", bad, pad);
            addIncompatibleToTooltip(tooltip, expanded);
            return;
        }

        List<SectorEntityToken> objects = getSurveyableInRange();
        if (s.detail == Settings.Detail.VAGUE) {
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
            // Same name and same found state count as one line ("Debris Field x4").
            Map<String, Integer> counts = new LinkedHashMap<>();
            Map<String, Color> colors = new LinkedHashMap<>();
            for (SectorEntityToken object : objects) {
                String name = s.detail == Settings.Detail.PRECISE ? preciseName(object) : object.getFullName();
                boolean grey = s.greyFound && !object.isDiscoverable();
                String key = (grey ? "1" : "0") + name;
                counts.merge(key, 1, Integer::sum);
                colors.putIfAbsent(key, grey ? gray : object.getIndicatorColor());
            }
            float currPad = 3f;
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                String label = countLabel(e.getKey().substring(1), e.getValue());
                tooltip.addPara("    %s", currPad, colors.get(e.getKey()), label);
                currPad = 0f;
            }
            if (s.greyFound) tooltip.addPara("Gray: already found.", gray, 3f);
        }

        if (s.showFleets) addFleets(tooltip, fleet, s.detail, pad, bad);

        if (s.activeScan) {
            String cost = s.costText();
            boolean canPay = canPay(fleet, s);
            String line = "Click to scan: stops the fleet for about 5 seconds and makes it easier to see, then "
                    + "discovers every object in this star system and names it on the map. Costs " + cost + ".";
            if (canPay) tooltip.addPara(line, pad, highlight, cost);
            else tooltip.addPara(line + " Not enough in your cargo.", bad, pad);
        }
        addIncompatibleToTooltip(tooltip, expanded);
    }

    static String countLabel(String name, int count) {
        return count > 1 ? name + " x" + count : name;
    }

    @Override
    public boolean hasTooltip() {
        return true;
    }

    // ---- Detail levels ----

    static final String VAGUE_SALVAGE = "Something here can still be salvaged.";
    static final String VAGUE_NONE = "Nothing here is left to salvage.";

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

    /** Fleets in the star system: every one but the player's and stations. */
    static List<CampaignFleetAPI> fleetsInRange(CampaignFleetAPI player) {
        List<CampaignFleetAPI> out = new ArrayList<>();
        LocationAPI location = player.getContainingLocation();
        if (location == null || location.isHyperspace()) return out;
        for (CampaignFleetAPI f : location.getFleets()) {
            if (f == player || f.isStationMode() || f.isPlayerFleet()) continue;
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
            tooltip.addPara("    %s", currPad, colors.get(e.getKey()), countLabel(e.getKey(), e.getValue()));
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

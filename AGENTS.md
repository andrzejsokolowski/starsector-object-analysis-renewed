# Object Analysis - Renewed

These instructions apply to this repository, including when it is opened without the parent mod workspace.
The workspace `AGENTS.md` one folder up still governs releases, packaging, Git identity and communication.

## Notion

Tasks, history and test lists: the **Object Analysis - Renewed board** in Andrzej's Notion, under
**Starsector mods** (`collection://41920ca7-5d20-8176-b1bc-000b53625723`). Ticket ids start with OAR.
Read the board with `notion-query-data-sources` mode `view` on `view://41920ca7-5d20-8164-bb0f-000c838cac3f`.
Working rules: **Claude Information** ("Start here: working on Starsector mods through Notion").

## What it is

A remake of Harpuea's Objects Analysis 002 (mod id `objects_analysis`), made on 2026-11-15 because its
ability cost about 7% of every campaign frame (memory watcher, OCL-4). Same ability id
`objectsanalysis`, so ability bar slots carry over. The original's `isUsable()` scanned the whole star
system every frame (O(n^2) with getEntitiesWithTag) and then returned false. Here `isUsable()` returns
false at once, and the object list is built only in `createTooltip`, each tag searched once.

- Behavior follows the shipped 002 jar, not its `src/` copy: a faint (`neutrino_low`) tag is skipped on a
  30% roll (the src file says 80%).
- The plugin takes the ability off before each save and puts it back after, as the original did, so
  saves never depend on this mod.
- With the original enabled too, `onApplicationLoad` stops the game with a message to turn it off: both
  define the ability, and the original's folder sorts later, so its slow row would win.

## Settings (1.1.0)

`Settings` reads LunaLib (`oar_detail` Radio Precise/Normal/Vague, `oar_fleets` Boolean) when the tooltip
opens; only `LunaReader` names LunaLib, so the mod runs without it on defaults (Precise, fleets on).
Precise names derelicts from `DerelictShipEntityPlugin.getData().ship` (shipName + hull
`getNameWithDesignationWithDashClass`), even when the game would hide the name. Vague: one line from the
`salvageable` tag and cargo pods. Fleets: all but the player's and station fleets; in hyperspace within
2000 units.

## 1.2.0

- List: every entity tagged neutrino, neutrino_low, neutrino_high or station, always (the original's 30% roll and
  qualifying check are gone, Andrzej: "This stupid system sucks"). Hidden: `HIDDEN_TYPES` (stellar_mirror,
  stellar_shade, fusion_lamp), `HIDDEN_TAGS` (niko_MPC_satellite, What We Left Behind), and station-tagged
  entities on a real colony market. Gates stay (Andrzej). Repeats grouped by name and found state.
- Active scan (`oar_activeScan`, default on): `isUsable` reads `Settings.now()` (cached 1 s, it runs every
  frame). activateImpl takes the cost (`oar_cost` volatiles / rare_metals / nothing, `oar_costAmount` 5);
  at full level `discoverAll` hands every discoverable entity in the system to the game's
  DiscoverEntityPlugin (map name, message, XP). abilities.csv: 0.2 / 0.2 / 0.1 days, 1 day cooldown.
- No list and no scan in hyperspace.

## Build

`./gradlew releaseZip test` compiles against the game, runs the checks and tests, and writes
`ObjectAnalysisRenewed.zip` in this folder. `gradle.properties` names the game folder.

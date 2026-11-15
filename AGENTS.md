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

## Build

`./gradlew releaseZip test` compiles against the game, runs the checks and tests, and writes
`ObjectAnalysisRenewed.zip` in this folder. `gradle.properties` names the game folder.

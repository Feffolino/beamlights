package it.ratlab.beamlights.server;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/** Game rules for the server features (per world, changed with /gamerule or /beamlights spawns|attract). */
public final class BeamGameRules {
    public static final String BLOCK_SPAWNS_NAME = "beamlightsBlockSpawns";
    public static final String ATTRACT_MOBS_NAME = "beamlightsAttractMobs";

    // Assigned in common setup (main thread): the game rule registry is a plain map, mods are constructed in parallel.
    private static GameRules.Key<GameRules.BooleanValue> blockSpawns;
    private static GameRules.Key<GameRules.BooleanValue> attractMobs;

    private BeamGameRules() {
    }

    public static void register() {
        if (blockSpawns != null) return;
        blockSpawns = GameRules.register(BLOCK_SPAWNS_NAME, GameRules.Category.SPAWNING,
                GameRules.BooleanValue.create(true));
        attractMobs = GameRules.register(ATTRACT_MOBS_NAME, GameRules.Category.MOBS,
                GameRules.BooleanValue.create(false));
    }

    public static boolean blockSpawns(Level level) {
        return blockSpawns != null && level.getGameRules().getBoolean(blockSpawns);
    }

    public static boolean attractMobs(Level level) {
        return attractMobs != null && level.getGameRules().getBoolean(attractMobs);
    }
}

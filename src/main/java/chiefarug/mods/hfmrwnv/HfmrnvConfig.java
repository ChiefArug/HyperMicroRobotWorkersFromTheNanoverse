package chiefarug.mods.hfmrwnv;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
import net.neoforged.neoforge.common.ModConfigSpec.DoubleValue;
import net.neoforged.neoforge.common.ModConfigSpec.IntValue;


// TODO Translations
public class HfmrnvConfig {
    private static final Builder BUILDER = new Builder();
    public static final IntValue SWARM_GENERATION_CHANCE = BUILDER.comment(
            "Each chunk has a 1/this chance of generating with a nanobot swarm",
            "A value of 1 means every chunk has a swarm",
            "The maximum value means that only about 6500 in the entire world will be infested",
            "For reference a world has about 14062500000000 chunks"
    ).defineInRange("swarm_generation_chance", 100, 1, Integer.MAX_VALUE);
    public static final IntValue CHUNK_SLOW_DOWN_FACTOR = BUILDER.comment(
            "The slow down factor for chunk swarms",
            "Swarms on chunks will tick once every this ticks",
            "No you can not make it faster than once per tick"
    ).defineInRange("chunk_slow_down", 10, 1, 72000); // max is one hour. if you need longer let me know
    public static final IntValue ENTITY_SLOW_DOWN_FACTOR = BUILDER.comment(
            "The slow down factor for entity (excluding player) swarms",
            "Swarms on entities will tick once every this ticks",
            "No you can not make it faster than once per tick"
    ).defineInRange("entity_slow_down", 1, 1, 72000);
    public static final IntValue PLAYER_SLOW_DOWN_FACTOR = BUILDER.comment(
            "The slow down factor for player swarms",
            "Swarms on players will tick once every this ticks",
            "No you can not make it faster than once per tick"
    ).defineInRange("player_slow_down", 1, 1, 72000);
    public static final DoubleValue HUNGER_EXHAUSTION = BUILDER.comment(
            "The amount of exhaustion applied to players, which gets multiplied by the effect level"
    ).defineInRange("hunger_effect_exhaustion", 0.05, 0, 40);
    public static final DoubleValue RAVENOUS_HUNGER_MULTIPLIER = BUILDER.comment(
            "The multiplier applied to hunger_effect_exhaustion for the ravenous affect"
    ).defineInRange("ravenous_multiplier", 5, 1d, 40);
    public static final IntValue HUNGER_CHUNK_DECAY = BUILDER.comment(
            "The number of blocks in each chunk section that can get converted per chunk nanobot tick"
    ).defineInRange("hunger_chunk_decay", 8, 0, 64);
    public static final DoubleValue ENTITY_SPREAD_CHANCE = BUILDER.comment(
            "The chance each time an spreading swarm ticks that it will spread to nearby entities"
    ).defineInRange("entity_spread_chance", 0.999, 0, 1);
    public static final DoubleValue ENTITY_SPREAD_DISTANCE = BUILDER.comment(
            "The distance that a swarm on an entity searches for other entities to spread to when it spreads"
    ).defineInRange("entity_spread_distance", 2, 0d, 16);
    public static final IntValue ENTITY_SPREAD_EXPOSURES = BUILDER.comment(
            "The number of exposures an entity needs to get infected by a swarm effect"
    ).defineInRange("entity_spread_exposures", 50, 1, Integer.MAX_VALUE);
    public static final DoubleValue CHUNK_SPREAD_CHANCE = BUILDER.comment(
            "The chance that a spreading swarm will try spread to a neighbouring chunk each time it ticks"
    ).defineInRange("chunk_spread_chance", 0.998, 0, 1);
    public static final IntValue CHUNK_SPREAD_EXPOSURES = BUILDER.comment(
            "The number of exposures a chunk needs to get infected by a swarm effect"
    ).defineInRange("chunk_spread_exposures", 5, 1, Integer.MAX_VALUE);
    static final ModConfigSpec SPEC = BUILDER.build();

}

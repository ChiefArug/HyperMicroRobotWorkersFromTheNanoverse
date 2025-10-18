package chiefarug.mods.hfmrwnv;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.Builder;
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
    public static final ModConfigSpec.DoubleValue HUNGER_EXHAUSTION = BUILDER.comment(
            "The amount of exhaustion applied to players, which gets multiplied by the effect level"
    ).defineInRange("consume_effect_exhaustion", 0.05, 0, 40);
    public static final ModConfigSpec.IntValue HUNGER_CHUNK_DECAY = BUILDER.comment(
            "The number of blocks in each chunk section that can get converted per chunk nanobot tick"
    ).defineInRange("hunger_chunk_decay", 8, 0, 64);
    static final ModConfigSpec SPEC = BUILDER.build();

}

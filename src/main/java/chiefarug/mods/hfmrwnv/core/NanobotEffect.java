package chiefarug.mods.hfmrwnv.core;

import chiefarug.mods.hfmrwnv.HfmrnvRegistries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import java.util.function.Function;

public interface NanobotEffect {
    Codec<NanobotEffect> CODEC = HfmrnvRegistries.EFFECTS.byNameCodec().dispatch(Function.identity(), NanobotEffect::codec);

    MapCodec<NanobotEffect> codec();
}

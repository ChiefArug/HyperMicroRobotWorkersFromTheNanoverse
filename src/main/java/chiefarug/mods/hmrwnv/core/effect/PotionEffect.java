package chiefarug.mods.hmrwnv.core.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/// Effect that gives affected entities a potion effect at the specified amplifier * level
public record PotionEffect(Holder<MobEffect> effect, int amplifier, int buffer) implements NanobotEffect.Ticking {
    public static final MapCodec<PotionEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(PotionEffect::effect),
            Codec.INT.fieldOf("amplifier").forGetter(PotionEffect::amplifier),
            Codec.INT.fieldOf("buffer_ticks").forGetter(PotionEffect::buffer)
    ).apply(inst, PotionEffect::new));
    
    @Override
    public MapCodec<PotionEffect> codec() {
        return CODEC;
    }

    @Override
    public void onTick(IAttachmentHolder host, int level) {
        if (!(host instanceof LivingEntity entity)) return;
        // Because amplifier starts at 0 for level one we need some weird math to make it level up properly.
        int finalAmplifier = amplifier + (level - 1) * (amplifier + 1);
        entity.addEffect(new MobEffectInstance(effect, NanobotEffect.getTickRate(host) + buffer, finalAmplifier, true, true));
    }
}

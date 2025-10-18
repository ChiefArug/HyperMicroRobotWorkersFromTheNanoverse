package chiefarug.mods.hfmrwnv.core.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public record PotionEffect(Holder<MobEffect> effect, int amplifier, int powerPerLevel) implements NanobotEffect.NonStateful {
    public static final MapCodec<PotionEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BuiltInRegistries.MOB_EFFECT.holderByNameCodec().fieldOf("effect").forGetter(PotionEffect::effect),
            Codec.INT.fieldOf("amplifier").forGetter(PotionEffect::amplifier),
            Codec.INT.fieldOf("powerFactor").forGetter(PotionEffect::powerPerLevel)
    ).apply(inst, PotionEffect::new));
    
    @Override
    public MapCodec<PotionEffect> codec() {
        return CODEC;
    }

    @Override
    public void onTick(IAttachmentHolder host, int level) {
        if (!(host instanceof LivingEntity entity)) return;
        entity.addEffect(new MobEffectInstance(effect, NanobotEffect.getTickRate(host) + 10, level * amplifier));
    }

    @Override
    public int getRequiredPower(int level) {
        return powerPerLevel * level;
    }
}

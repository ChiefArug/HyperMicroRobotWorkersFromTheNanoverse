package chiefarug.mods.hmrwnv.core;

import chiefarug.mods.hmrwnv.RegistryInjectionXMapCodec;
import chiefarug.mods.hmrwnv.client.ClientEffect;
import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

import java.util.Objects;
import java.util.Optional;

import static chiefarug.mods.hmrwnv.HmrnvRegistries.EFFECTS_KEY;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL_CODEC;

/// A holder for {@link NanobotEffect NanobotEffects} that holds universal information.
/// @param <T> The type of effect. Usually ? as we don't care about it.
/// @param effect The effect itself
/// @param energyPerLevel The energy required or provided by this effect per level. 0 means no cost and negative means it provides energy
/// @param maxLevel The maximum level for this effect. Note that not all effects actually support levels.
/// @param color The RGB color of this effect used for display in some places.
public record EffectConfiguration<T extends NanobotEffect>(T effect, int energyPerLevel, int maxLevel, int color) {
    /// A codec that supports more number formats in the form of strings
    /// Always writes to a raw int.
    public static final Codec<Integer> HEXADECIMAL_INT = Codec.withAlternative(Codec.INT, Codec.STRING.xmap(Integer::decode, i -> '#' + Integer.toString(i, 16)));
    public static final Codec<EffectConfiguration<?>> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                    NanobotEffect.CODEC.fieldOf("effect").forGetter(EffectConfiguration::effect),
                    Codec.INT.fieldOf("energy_factor").forGetter(EffectConfiguration::energyPerLevel),
                    Codec.INT.optionalFieldOf("max_level").xmap(o -> o.orElse(Integer.MAX_VALUE), i -> i == Integer.MAX_VALUE ? Optional.empty() : Optional.of(i)).forGetter(EffectConfiguration::maxLevel),
                    HEXADECIMAL_INT.fieldOf("color").forGetter(EffectConfiguration::color)
            ).apply(inst, EffectConfiguration::new));
    public static final Codec<Holder<EffectConfiguration<?>>> BY_ID_CODEC = RegistryInjectionXMapCodec.createRegistryValue(MODRL_CODEC, EFFECTS_KEY);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<EffectConfiguration<?>>> BY_ID_STREAM_CODEC = ByteBufCodecs.holderRegistry(EFFECTS_KEY);
    /// Deprecated for removal as soon as mojang makes datapack registries use stream codecs
    public static final Codec<EffectConfiguration<?>> CLIENT_CODEC;
    static {
        // for some reason the type inferencer just dies if it has to infer that <NanobotEffect> can be <?> after going through codec()
        // so we help it along by providing an intermediate variable.
        MapCodec<EffectConfiguration<?>> typeSummoningRitual = HEXADECIMAL_INT.fieldOf("color").xmap(EffectConfiguration::new, EffectConfiguration::color);
        CLIENT_CODEC = typeSummoningRitual.codec();
    }
    /// Constructor used only on the client side which needs much less information
    @SuppressWarnings("unchecked")
    public EffectConfiguration(int color) {
        this((T) ClientEffect.Guard.getInstance(), 0, Integer.MAX_VALUE, color);
    }

    /// @see NanobotEffect#onAdd(IAttachmentHolder, int)
    public void onAdd(IAttachmentHolder host, int level) {
        effect.onAdd(host, level);
    }

    /// @see NanobotEffect#onRemove(IAttachmentHolder, int)
    public void onRemove(IAttachmentHolder host, int level) {
        effect.onRemove(host, level);
    }

    /// @see NanobotEffect#onTick(IAttachmentHolder, int)
    public void onTick(IAttachmentHolder host, int level) {
        effect.onTick(host, level);
    }

    /// @see NanobotEffect#affectsEntitiesInChunk()
    public boolean affectsEntitiesInChunk() {
        return effect.affectsEntitiesInChunk();
    }

    public int getRequiredPower(int level) {
        return energyPerLevel * level;
    }

    public static ResourceLocation id(Holder<EffectConfiguration<?>> self) {
        return Objects.requireNonNull(self.getKey()).location();
    }

    public static MutableComponent name(Holder<EffectConfiguration<?>> self) {
        return Component.translatable(id(self).toLanguageKey(EFFECTS_KEY.location().getPath()).replace('/', '.'));
    }

    public static MutableComponent description(Holder<EffectConfiguration<?>> self) {
        return Component.translatable(id(self).toLanguageKey(EFFECTS_KEY.location().getPath()).replace('/', '.') + ".description");
    }

    public static MutableComponent nameWithLevel(Holder<EffectConfiguration<?>> self, int level) {
        return Component.translatable("hmrw_nanoverse.effect_level.formatting",
                name(self), level(level));
    }

    static MutableComponent level(int i) {
        return Component.translatable("hmrw_nanoverse.effect_level." + i);
    }

    public int colorWithTransparency(int transparency) {
        return transparency << 24 | color;
    }

    public int colorWithTransparency() {
        return colorWithTransparency(0xFF);
    }
}

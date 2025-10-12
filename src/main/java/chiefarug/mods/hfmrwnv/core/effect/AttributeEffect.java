package chiefarug.mods.hfmrwnv.core.effect;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

public record AttributeEffect(Holder<Attribute> attribute, ResourceLocation id, double baseAmount, AttributeModifier.Operation operation) implements NanobotEffect.Constant {
    public static final MapCodec<AttributeEffect> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BuiltInRegistries.ATTRIBUTE.holderByNameCodec().fieldOf("attribute").forGetter(AttributeEffect::attribute),
            AttributeModifier.MAP_CODEC.forGetter(AttributeEffect::asModifier)
    ).apply(inst, AttributeEffect::new));

    private AttributeEffect(Holder<Attribute> attribute, AttributeModifier modifier) {
        this(attribute, modifier.id(), modifier.amount(), modifier.operation());
    }

    private AttributeModifier asModifier() {
        return asModifier(1);
    }

    private AttributeModifier asModifier(int level) {
        return new AttributeModifier(id, baseAmount * level, operation);
    }

    @Override
    public MapCodec<AttributeEffect> codec() {
        return CODEC;
    }

    @Override
    public void onAdd(IAttachmentHolder holder, int level) {
        if (holder instanceof LivingEntity entity) {
            entity.getAttributes().addTransientAttributeModifiers(ImmutableMultimap.of(attribute, asModifier(level)));
        }
    }

    @Override
    public void onRemove(IAttachmentHolder holder, int level) {
        if (holder instanceof LivingEntity entity) {
            entity.getAttributes().removeAttributeModifiers(ImmutableMultimap.of(attribute, asModifier(level)));
        }
    }
}

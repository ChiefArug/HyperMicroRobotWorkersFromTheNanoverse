package chiefarug.mods.hmrwnv.core.collections;

import chiefarug.mods.hmrwnv.core.EffectConfiguration;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.AbstractObject2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.List;

/// Essentially a type alias for {@link Object2IntMap Object2IntMap<Holder<EffectConfiguration<?>>>} because who can be bothered typing that.
/// Also provides helpers for some more efficient methods relating to collections of entries.
/// @apiNote If you want more impls of this come yell at me.
public sealed interface EffectMap extends Object2IntMap<Holder<EffectConfiguration<?>>>
        permits EffectArrayMap, EffectMap.Empty, EffectMap.Singleton, EffectMap.Unmodifiable {

    Codec<Entry<Holder<EffectConfiguration<?>>>> ENTRY_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            EffectConfiguration.BY_ID_CODEC.fieldOf("effect").forGetter(Entry::getKey),
            Codec.INT.fieldOf("level").forGetter(Entry::getIntValue)
    ).apply(inst, AbstractObject2IntMap.BasicEntry::new));
    Codec<EffectMap> CODEC = Codec.list(ENTRY_CODEC).xmap(EffectArrayMap::new, EffectMap::asUnmodifiableList);
    StreamCodec<RegistryFriendlyByteBuf, EffectMap> EFFECTS_STREAM_CODEC = ByteBufCodecs.map(EffectArrayMap::new, EffectConfiguration.BY_ID_STREAM_CODEC, ByteBufCodecs.INT);

    @org.jetbrains.annotations.Unmodifiable
    static EffectMap unmodifiable(EffectMap map) {
        if (map instanceof Unmodifiable || map instanceof Singleton) return map;
        return new Unmodifiable(map);
    }

    @org.jetbrains.annotations.Unmodifiable
    static EffectMap singleton(Holder<EffectConfiguration<?>> key, int value) {
        return new Singleton(key, value);
    }

    EffectMap EMPTY = new Empty();

    @org.jetbrains.annotations.Unmodifiable
    static EffectMap empty() {
        return EMPTY;
    }

    /// Helper method to get a list view of this map.
    /// This only works because all the implementations of this can be easily represented in a list view
    @UnmodifiableView
    List<Entry<Holder<EffectConfiguration<?>>>> asUnmodifiableList();

    /// Returns the summed power of all effects in this map.
    int totalPower();

    final class Empty extends Object2IntMaps.EmptyMap<Holder<EffectConfiguration<?>>> implements EffectMap {
        @Override
        public List<Entry<Holder<EffectConfiguration<?>>>> asUnmodifiableList() {
            return Collections.emptyList();
        }

        @Override
        public int totalPower() {
            return 0;
        }
    }

    final class Unmodifiable extends Object2IntMaps.UnmodifiableMap<Holder<EffectConfiguration<?>>> implements EffectMap {
        Unmodifiable(EffectMap m) { super(m); }

        @Override
        public List<Entry<Holder<EffectConfiguration<?>>>> asUnmodifiableList() {
            return ((EffectMap) map).asUnmodifiableList();
        }

        @Override
        public int totalPower() {
            return ((EffectMap) map).totalPower();
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }

    final class Singleton extends Object2IntMaps.Singleton<Holder<EffectConfiguration<?>>> implements EffectMap {
        Singleton(Holder<EffectConfiguration<?>> effect, int level) {
            super(effect, level);
        }

        @Override
        public List<Entry<Holder<EffectConfiguration<?>>>> asUnmodifiableList() {
            return List.of(new AbstractObject2IntMap.BasicEntry<>(key, value));
        }

        @Override
        public int totalPower() {
            return key.value().getRequiredPower(value);
        }

        @Override
        public String toString() {
            return "{" + stringifyKey(key) + "=>" + value + "}";
        }
    }


    static String stringifyKey(Holder<EffectConfiguration<?>> key) {
        return key.unwrapKey().map(ResourceKey::location).map(ResourceLocation::toString).orElseGet(key::toString);
    }
}

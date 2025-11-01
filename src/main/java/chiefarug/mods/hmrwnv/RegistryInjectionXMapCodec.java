package chiefarug.mods.hmrwnv;

import chiefarug.mods.hmrwnv.mixin.RegistryCracker;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;
import java.util.function.BiFunction;

import static net.minecraft.SharedConstants.IS_RUNNING_IN_IDE;

/// Injects a HolderGetter for a specific registry into the apply/unapply functions
/// Also includes a utility method for cracking open said holder getter into a full registry
public record RegistryInjectionXMapCodec<B, A>(
        ResourceKey<? extends Registry<A>> reg,
        Codec<B> base,
        BiFunction<HolderGetter<A>, B, Holder<A>> apply,
        BiFunction<HolderGetter<A>, Holder<A>, B> unapply) implements Codec<Holder<A>> {

    public static <A> RegistryInjectionXMapCodec<ResourceLocation, A> createRegistryValue(ResourceKey<? extends Registry<A>> reg) {
        return createRegistryValue(ResourceLocation.CODEC, reg);
    }

    public static <A> RegistryInjectionXMapCodec<ResourceLocation, A> createRegistryValue(Codec<ResourceLocation> modrlCodec, ResourceKey<? extends Registry<A>> reg) {
        return create(reg, modrlCodec,
                (hg, rl) -> hg.get(ResourceKey.create(reg, rl)).orElseThrow(),
                (hg, a) -> a.unwrap().map(ResourceKey::location, v -> crack(hg).getKey(v)));
    }

    public static <B, A> RegistryInjectionXMapCodec<B, A> create(ResourceKey<? extends Registry<A>> reg, Codec<B> base, BiFunction<HolderGetter<A>, B, Holder<A>> apply, BiFunction<HolderGetter<A>, Holder<A>, B> unapply) {
        return new RegistryInjectionXMapCodec<B, A>(reg, base, apply, unapply);
    }


    @Override
    public <T> DataResult<Pair<Holder<A>, T>> decode(DynamicOps<T> ops, T input) {
        if (ops instanceof RegistryOps<T> rops) {
            DataResult<Pair<B, T>> decoded = base.decode(ops, input);
            Optional<HolderGetter<A>> getter = rops.lookupProvider.lookup(reg).map(RegistryOps.RegistryInfo::getter);
            if (decoded.isError())
                decoded = decoded.mapError(s -> "Couldn't decode with registry due to inner error: " + s);
            if (getter.isEmpty())
                return DataResult.error(() -> "Registry " + reg + " holder getter not found!");

            return decoded.map(p -> Pair.of(apply.apply(getter.get(), p.getFirst()), p.getSecond()));
        }
        return DataResult.error(() -> "Cannot decode RegistryInjectionCodec without a RegistryOps!");
    }

    @Override
    public <T> DataResult<T> encode(Holder<A> input, DynamicOps<T> ops, T prefix) {
        if (ops instanceof RegistryOps<T> rops) {
            Optional<HolderGetter<A>> getter = rops.lookupProvider.lookup(reg).map(RegistryOps.RegistryInfo::getter);
            if (getter.isEmpty())
                return DataResult.error(() -> "Registry " + reg + " holder getter not found!");

            B midResult = unapply.apply(getter.get(), input);
            return base.encode(midResult, ops, prefix);
        }
        return DataResult.error(() -> "Cannot encode RegistryInjectionCodec without a RegistryOps!");
    }

    @Override
    public String toString() {
        return "RegistryInjectCodec[" + base + " through " + apply + ',' + unapply + ']';
    }

    public static <T> Registry<T> crack(HolderGetter<T> hg) {
        if (IS_RUNNING_IN_IDE) throw new UnsupportedOperationException("This will be removed at some point! fix the direct holderness!");

        if (hg instanceof RegistryCracker<T> cracked) return cracked.hmrw_nanoverse$crack();
        throw new IllegalStateException("Failed to crack open HolderGetter into full registry! Unexpected implementation of HolderGetter: " + hg.getClass().getName());
    }
}

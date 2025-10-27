package chiefarug.mods.hmrwnv.mixin;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;

/// Cracks open a HolderGetter to get out the registry backing it.
/// Has a few implementations that crack open different implementations of HolderGetter.
/// Not all default implementations are crackable, only those that are easy to crack are. We should in theory not encounter any of the others anyway.
///
/// Yes I'm abusing an accessor as a duck interface. live with it.
/// @see chiefarug.mods.hmrwnv.RegistryInjectionXMapCodec#crack(HolderGetter) crack
@Mixin(targets = "net.minecraft.core.Registry$2")
public interface RegistryCracker<T> extends HolderGetter<T> {

    @Accessor("this$0")
    Registry<T> hmrw_nanoverse$crack();

    @Mixin(targets = "net.minecraft.core.MappedRegistry$1")
    abstract class MappedRegistryCracker<T> implements RegistryCracker<T> {
        @Shadow
        @Final MappedRegistry<T> this$0;

        // this cannot be a simple accessor due to @Accessor not supporting coerce,
        // and mixin not liking the synthetic method generated from changing return type
        @Override
        public Registry<T> hmrw_nanoverse$crack() {
            return this$0;
        }
    }

    @Mixin(HolderLookup.RegistryLookup.Delegate.class)
    interface DelegateCracker<T> extends RegistryCracker<T> {
        @Shadow
        HolderLookup.RegistryLookup<T> parent();

        @Override
        default Registry<T> hmrw_nanoverse$crack() {
            HolderLookup.RegistryLookup<T> parent = parent();
            if ((HolderGetter<T>) parent instanceof RegistryCracker<T> cracked) return cracked.hmrw_nanoverse$crack();
            throw new IllegalStateException("Failed to crack open parent HolderGetter into full registry! Unexpected implementation: " + parent.getClass().getName());
        };
    }
}

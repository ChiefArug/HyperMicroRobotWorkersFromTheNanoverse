package chiefarug.mods.hmrwnv.core.collections;

import chiefarug.mods.hmrwnv.core.EffectConfiguration;
import it.unimi.dsi.fastutil.objects.AbstractObjectList;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.LGGR;
import static net.minecraft.SharedConstants.IS_RUNNING_IN_IDE;

public final class EffectArrayMap extends Object2IntArrayMap<Holder<EffectConfiguration<?>>> implements EffectMap {

    public EffectArrayMap() {
        super();
    }

    public EffectArrayMap(int capacity) {
        super(capacity);
    }

    public EffectArrayMap(Object[] key, int[] value) {
        super(key, value);
    }

    public EffectArrayMap(Object[] key, int[] value, int size) {
        super(key, value, size);
    }

    public EffectArrayMap(Map<? extends Holder<EffectConfiguration<?>>, ? extends Integer> m) {
        super(m);
    }

    public EffectArrayMap(EffectMap m) {
        super(m);
    }

    public EffectArrayMap(Collection<Object2IntMap.Entry<Holder<EffectConfiguration<?>>>> es) {
        this(es.size());
        int i = 0;
        Iterator<Object2IntMap.Entry<Holder<EffectConfiguration<?>>>> iterator = es instanceof Object2IntMap.FastEntrySet<Holder<EffectConfiguration<?>>> f ? f.fastIterator() : es.iterator();
        while (iterator.hasNext()) {
            Object2IntMap.Entry<Holder<EffectConfiguration<?>>> e = iterator.next();
            key[i] = e.getKey();
            value[i] = e.getIntValue();
            i++;
        }
        size = i;
    }

    @Override
    public int put(Holder<EffectConfiguration<?>> effect, int level) {
        if (level <= 0) {
            if (IS_RUNNING_IN_IDE)
                throw new RuntimeException("Effect level must be positive integer! But was " + level);
            else
                LGGR.error("Effect level must be positive integer! But effect {} attempted to have level {}. Aborting put operation", level, new Throwable());
            return getInt(effect);
        }
        return super.put(effect, level);
    }

    @Override
    public List<Entry<Holder<EffectConfiguration<?>>>> asUnmodifiableList() {
        return new AbstractObjectList<>() {
            @Override
            public int size() {
                return EffectArrayMap.this.size();
            }

            @Override
            public int indexOf(Object k) {
                if (!(k instanceof Object2IntMap.Entry<?> e)) return -1;
                int size = size();
                for (int i = 0; i < size; i++) {
                    // because of the guarantee of the map, the key can only match once
                    if (key[i].equals(e.getKey())) {
                        if (value[i] == e.getIntValue())
                            return i;
                        return -1;
                    }
                }
                return -1;
            }

            @Override // because of the key guarantee of maps, the object can only be contained once so indexOf == lastIndexOf
            public int lastIndexOf(Object k) {
                return indexOf(k);
            }

            @Override
            public Entry<Holder<EffectConfiguration<?>>> get(int index) {
                //noinspection unchecked // it is safe as we garuntee all inserts.
                return new BasicEntry<>((Holder<EffectConfiguration<?>>) key[index], value[index]);
            }
        };
    }


    @Override
    public String toString() {
        // 30 assumes an average length of id of 26, and given our modid is 14 long that gives 12 for the path
        // as of writing our average path length is 12.08.
        StringBuilder builder = new StringBuilder(2 + this.size() * 31);
        builder.append('{');
        var entries = this.asUnmodifiableList();
        int i = 0;
        while (true) {
            @SuppressWarnings("unchecked") // safe because we guard all entry attempts
            Holder<EffectConfiguration<?>> key = (Holder<EffectConfiguration<?>>) this.key[i];
            int value = this.value[i];
            builder.append(EffectMap.stringifyKey(key))
                    .append("=>")
                    .append(value);
            if (++i < entries.size())
                builder.append(',');
            else
                break;
        }
        builder.append('}');
        return builder.toString();
    }

}

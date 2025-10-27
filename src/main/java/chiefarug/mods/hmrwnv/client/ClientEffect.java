package chiefarug.mods.hmrwnv.client;

import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/// An implementation of NanobotEffect that exist purely to represent effects on the client.
/// deprecated for removal when mojang finally makes datapack registries use a proper stream codec.
public class ClientEffect implements NanobotEffect {
    // in theory this prevents ClientEffect from loading until the codec is actually called.
    public static class Guard {
        @SuppressWarnings("Convert2MethodRef")
        public static final MapCodec<NanobotEffect> CODEC = MapCodec.unit(() -> getInstance());
    }

    // return a new instance each time to make suer that value -> id mapping still works.
    private static ClientEffect getInstance() {
        return new ClientEffect();
    }

    @Override
    public MapCodec<? extends NanobotEffect> codec() {
        return Guard.CODEC;
    }

    @Override
    public void onAdd(IAttachmentHolder host, int level) {}

    @Override
    public void onRemove(IAttachmentHolder host, int level) {}

    @Override
    public void onTick(IAttachmentHolder host, int level) {}

    @Override
    public int getRequiredPower(int level) {
        return 0;
    }
}

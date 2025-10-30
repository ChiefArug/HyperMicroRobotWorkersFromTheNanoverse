package chiefarug.mods.hmrwnv.client;

import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.attachment.IAttachmentHolder;

/// An implementation of NanobotEffect that exist purely to represent effects on the client.
/// deprecated for removal when mojang finally makes datapack registries use a proper stream codec.
public class ClientEffect implements NanobotEffect {
    // in theory this prevents ClientEffect from loading until the codec is actually called.
    public static class Guard {
        public static final MapCodec<NanobotEffect> CODEC = MapCodec.unit(Guard::getInstance);
        public static NanobotEffect getInstance() {
            return ClientEffect.getInstance();
        }
    }

    private static final ClientEffect INSTANCE = new ClientEffect();
    private static ClientEffect getInstance() {
        return INSTANCE;
    }

    @Override
    public MapCodec<? extends NanobotEffect> codec() {
        throw new UnsupportedOperationException("Cannot serialize on the client");
    }

    @Override
    public void onAdd(IAttachmentHolder host, int level) {
        throw new UnsupportedOperationException("Cannot add effects on the client");
    }

    @Override
    public void onRemove(IAttachmentHolder host, int level) {
        throw new UnsupportedOperationException("Cannot remove effects on the client");
    }

    @Override
    public void onTick(IAttachmentHolder host, int level) {
        throw new UnsupportedOperationException("Cannot tick on the client");
    }
}

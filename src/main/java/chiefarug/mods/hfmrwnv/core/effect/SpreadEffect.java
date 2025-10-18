package chiefarug.mods.hfmrwnv.core.effect;

import net.neoforged.neoforge.attachment.IAttachmentHolder;

public class SpreadEffect implements NanobotEffect.NonStateful, NanobotEffect.Unit {

    @Override
    public void onTick(IAttachmentHolder host, int level) {

    }

    @Override
    public int getRequiredPower(int level) {
        return level * 4;
    }
}

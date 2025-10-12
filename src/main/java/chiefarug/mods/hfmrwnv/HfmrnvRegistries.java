package chiefarug.mods.hfmrwnv;

import chiefarug.mods.hfmrwnv.block.NanobotTableBlockEntity;
import chiefarug.mods.hfmrwnv.core.NanobotEffect;
import chiefarug.mods.hfmrwnv.core.NanobotSwarm;
import chiefarug.mods.hfmrwnv.item.NanobotItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

import java.util.Set;

import static chiefarug.mods.hfmrwnv.HyperFungusMicroRobotWorkersFromTheNanoVerse.MODID;
import static chiefarug.mods.hfmrwnv.HyperFungusMicroRobotWorkersFromTheNanoVerse.MODRL;

public class HfmrnvRegistries {
    @SuppressWarnings("unchecked")
    private static final DeferredRegister<Registry<?>> REGISTRIES = DeferredRegister.create((Registry<Registry<?>>) BuiltInRegistries.REGISTRY, MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, MODID);
    private static final DeferredRegister<AttachmentType<?>> DATA_ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);

    public static final Registry<NanobotEffect> EFFECTS = new RegistryBuilder<NanobotEffect>(ResourceKey.createRegistryKey(MODRL.withPath("effects")))
            .sync(true)
            .create();

    private static final DeferredRegister<NanobotEffect> NANOBOT_EFFECTS = DeferredRegister.create(EFFECTS, MODID);
    public static final DeferredHolder<NanobotEffect, NanobotEffect> EFFECT = NANOBOT_EFFECTS.register("effect", () -> () -> null);

    public static final DeferredBlock<Block> NANOBOT_TABLE = BLOCKS.registerSimpleBlock("nanobot_assembly_table", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NanobotTableBlockEntity>> NANOBOT_TABLE_BE = BLOCK_ENTITY_TYPES.register("nanobot_table", () -> new BlockEntityType<>(NanobotTableBlockEntity::new, Set.of(NANOBOT_TABLE.get()), null));
           static {ITEMS.registerSimpleBlockItem(NANOBOT_TABLE);}
    public static final DeferredItem<NanobotItem> NANOBOT = ITEMS.register("nanobot", () -> new NanobotItem(new Item.Properties()));

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<NanobotSwarm>> SWARM = DATA_ATTACHMENTS.register("nanobot_swarm", AttachmentType
            .<NanobotSwarm>builder(_t -> {throw new IllegalStateException("No default value. Use hasData to check presence before getting!");})
            .serialize(NanobotSwarm.CODEC)
            .sync(NanobotSwarm.STREAM_CODEC)
            ::build);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hfmrw_nanoverse"))
            .icon(() -> NANOBOT_TABLE.asItem().getDefaultInstance())
            .displayItems(ITEMS.getEntries())
            .build());

    
    
    
    
    
    
    
    
    
    
    static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        REGISTRIES.register(modBus);
        DATA_ATTACHMENTS.register(modBus);
        NANOBOT_EFFECTS.register(modBus);

        modBus.addListener((NewRegistryEvent event) -> event.register(EFFECTS));
    }
}

package chiefarug.mods.hfmrwnv;

import chiefarug.mods.hfmrwnv.block.NanobotTableBlockEntity;
import chiefarug.mods.hfmrwnv.core.NanobotSwarm;
import chiefarug.mods.hfmrwnv.core.effect.AttributeEffect;
import chiefarug.mods.hfmrwnv.core.effect.NanobotEffect;
import chiefarug.mods.hfmrwnv.item.NanobotItem;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);

    public static final Registry<NanobotEffect> EFFECTS = new RegistryBuilder<NanobotEffect>(ResourceKey.createRegistryKey(MODRL.withPath("effects")))
            .sync(true)
            .create();

    private static final DeferredRegister<NanobotEffect> NANOBOT_EFFECTS = DeferredRegister.create(EFFECTS, MODID);
    public static final DeferredHolder<NanobotEffect, AttributeEffect> MAX_HEALTH = NANOBOT_EFFECTS.register("attribute", () -> new AttributeEffect(Attributes.MAX_HEALTH, MODRL.withPath("max_health"), 1, AttributeModifier.Operation.ADD_VALUE));

    public static final DeferredBlock<Block> NANOBOT_TABLE = BLOCKS.registerSimpleBlock("nanobot_assembly_table", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NanobotTableBlockEntity>> NANOBOT_TABLE_BE = BLOCK_ENTITY_TYPES.register("nanobot_table", () -> new BlockEntityType<>(NanobotTableBlockEntity::new, Set.of(NANOBOT_TABLE.get()), null));
           static {ITEMS.registerSimpleBlockItem(NANOBOT_TABLE);}
    public static final DeferredItem<NanobotItem> NANOBOT = ITEMS.register("nanobot", () -> new NanobotItem(new Item.Properties()));
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hfmrw_nanoverse"))
            .icon(() -> NANOBOT.asItem().getDefaultInstance())
            .displayItems(ITEMS.getEntries())
            .build());

    public static final DataEverything<NanobotSwarm> SWARM = new DataEverything<>("swarm", NanobotSwarm.CODEC, NanobotSwarm.STREAM_CODEC);


    public record DataEverything<T>(DeferredHolder<DataComponentType<?>, DataComponentType<T>> dc, DeferredHolder<AttachmentType<?>, AttachmentType<T>> at) {
        public DataEverything(String name, Codec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
            this(
                    DATA_COMPONENTS.registerComponentType(name, b -> b.persistent(codec).networkSynchronized(streamCodec)),
                    DATA_ATTACHMENTS.register(name, AttachmentType.builder(HfmrnvRegistries::<T>justThrow).serialize(codec).sync(streamCodec)::build)
            );
        }
        public DataComponentType<T> component() {
            return dc.get();
        }
        public AttachmentType<T> attachment() {
            return at.get();
        }
    }






    static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        REGISTRIES.register(modBus);
        DATA_ATTACHMENTS.register(modBus);
        NANOBOT_EFFECTS.register(modBus);

        modBus.addListener((NewRegistryEvent event) -> event.register(EFFECTS));
    }

    private static <T> T justThrow(Object _t) {
        throw new IllegalStateException("No default value. Use hasData to check presence before getting, or use one of the getExistingData methods!");
    }
}

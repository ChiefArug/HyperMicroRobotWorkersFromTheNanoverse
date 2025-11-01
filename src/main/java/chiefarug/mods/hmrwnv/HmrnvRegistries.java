package chiefarug.mods.hmrwnv;

import chiefarug.mods.hmrwnv.block.NanobotDiffuserBlock;
import chiefarug.mods.hmrwnv.block.NanobotDiffuserBlockEntity;
import chiefarug.mods.hmrwnv.core.EffectConfiguration;
import chiefarug.mods.hmrwnv.core.NanobotSwarm;
import chiefarug.mods.hmrwnv.core.collections.EffectMap;
import chiefarug.mods.hmrwnv.core.effect.AttributeEffect;
import chiefarug.mods.hmrwnv.core.effect.HungerEffect;
import chiefarug.mods.hmrwnv.core.effect.NanobotEffect;
import chiefarug.mods.hmrwnv.core.effect.PotionEffect;
import chiefarug.mods.hmrwnv.core.effect.RavenousEffect;
import chiefarug.mods.hmrwnv.core.effect.SpreadEffect;
import chiefarug.mods.hmrwnv.item.GogglesItem;
import chiefarug.mods.hmrwnv.item.NanobotItem;
import chiefarug.mods.hmrwnv.recipe.NanobotAddEffectRecipe;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODID;
import static chiefarug.mods.hmrwnv.HyperMicroRobotWorkersFromTheNanoverse.MODRL;
import static chiefarug.mods.hmrwnv.core.collections.EffectMap.EFFECTS_STREAM_CODEC;

public class HmrnvRegistries {
    //<editor-fold desc="Registries">
    public static final ResourceKey<Registry<EffectConfiguration<?>>> EFFECTS_KEY = ResourceKey.createRegistryKey(MODRL.withPath("effect"));
    public static final Registry<MapCodec<? extends NanobotEffect>> EFFECT_CODEC_REG = new RegistryBuilder<MapCodec<? extends NanobotEffect>>(ResourceKey.createRegistryKey(MODRL.withPath("effect_codec"))).create();
    //</editor-fold>

    //<editor-fold desc="DeferredRegisters">
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(BuiltInRegistries.CREATIVE_MODE_TAB, MODID);
    private static final DeferredRegister<AttachmentType<?>> DATA_ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MODID);
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MODID);
    private static final DeferredRegister<MapCodec<? extends NanobotEffect>> EFFECT_CODECS = DeferredRegister.create(EFFECT_CODEC_REG, MODID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
    //</editor-fold>

    //<editor-fold desc="NanobotEffect Codecs">
            static { EFFECT_CODECS.register("attribute", () -> AttributeEffect.CODEC); }
            static { EFFECT_CODECS.register("potion", () -> PotionEffect.CODEC); }
            static { EFFECT_CODECS.register("hunger", () -> HungerEffect.CODEC); }
            static { EFFECT_CODECS.register("ravenous", () -> RavenousEffect.CODEC); }
            static { EFFECT_CODECS.register("spread", () -> SpreadEffect.CODEC); }
            static { EFFECT_CODECS.register("empty", () -> NanobotEffect.Empty.CODEC); }
    //</editor-fold>

    //<editor-fold desc="Tags & Taglikes">
    public static final TagKey<Item> BOT_VISION_ITEM = ITEMS.createTagKey("nanobot_vision");
    public static final TagKey<EffectConfiguration<?>> BOT_VISION_EFFECT = TagKey.create(EFFECTS_KEY, MODRL.withPath("nanobot_vision"));
    public static final ItemAbility BOT_VISION = ItemAbility.get("hmrw_nanoverse:bot_vision");
    public static final TagKey<Block> RAVENOUS_BLACKLIST = BLOCKS.createTagKey("ravenous_blacklist");
    public static final TagKey<EffectConfiguration<?>> PREVENTS_RANDOM_TICKS = TagKey.create(EFFECTS_KEY, MODRL.withPath("prevents_random_ticks"));
    public static final TagKey<EffectConfiguration<?>> PROTECTS_AGAINST_SPREAD = TagKey.create(EFFECTS_KEY, MODRL.withPath("protects_against_spread"));
    //</editor-fold>

    //<editor-fold desc="Blocks">
    public static final DeferredBlock<Block> SLAG = BLOCKS.registerSimpleBlock("slag", BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK));
           static {ITEMS.registerSimpleBlockItem(SLAG);}
    public static final DeferredBlock<Block> RICH_SLAG = BLOCKS.registerSimpleBlock("rich_slag", BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK));
           static {ITEMS.registerSimpleBlockItem(RICH_SLAG);}
    public static final DeferredBlock<NanobotDiffuserBlock> NANOBOT_DIFFUSER = BLOCKS.registerBlock("nanobot_diffuser", NanobotDiffuserBlock::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NanobotDiffuserBlockEntity>> NANOBOT_DIFFUSER_BE = BLOCK_ENTITY_TYPES.register("nanobot_diffuser", () -> BlockEntityType.Builder.of(NanobotDiffuserBlockEntity::new, NANOBOT_DIFFUSER.get()).build(null));
           static {ITEMS.registerSimpleBlockItem(NANOBOT_DIFFUSER);}
    //</editor-fold>

    //<editor-fold desc="Items">
    public static final DeferredItem<Item> NANOBOT = ITEMS.registerSimpleItem("nanobot");
           static { ITEMS.registerSimpleItem("slop"); }
           static { ITEMS.registerSimpleItem("llm"); }
           static { ITEMS.registerSimpleItem("ai"); }
           static { ITEMS.registerSimpleItem("ml"); }
    public static final DeferredItem<NanobotItem> NANOBOTS = ITEMS.registerItem("nanobots", NanobotItem::new);
           static { ITEMS.registerItem("nanobot_goggles", GogglesItem::new, new Item.Properties().stacksTo(1)); }
    //</editor-fold>

    //<editor-fold desc="Misc">
           static { RECIPE_SERIALIZERS.register("add_effect", () -> NanobotAddEffectRecipe.INSTANCE); }
           static { TABS.register("tab", CreativeModeTab.builder()
                   .title(Component.translatable("itemGroup.hmrw_nanoverse"))
                   .icon(() -> NANOBOT.asItem().getDefaultInstance())
                   .displayItems(HmrnvRegistries.getAllItems())
                   ::build);
           }

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Object2IntMap<ResourceLocation>>> INFECTION = DATA_ATTACHMENTS.register("infection",
            AttachmentType.<Object2IntMap<ResourceLocation>>builder(() -> new Object2IntOpenHashMap<>())
                    .serialize(Codec.unboundedMap(ResourceLocation.CODEC, Codec.INT).xmap(Object2IntOpenHashMap::new, Function.identity()))
                    .sync(ByteBufCodecs.map(Object2IntOpenHashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.INT))
                    ::build);

    public static final Swarm SWARM = new Swarm(EffectMap.CODEC, EFFECTS_STREAM_CODEC, DATA_ATTACHMENTS.register("swarm", AttachmentType
            .<NanobotSwarm>builder(_t -> { throw new IllegalStateException("No default value. Use hasData to check presence before getting, or use one of the getExistingData methods!"); })
            .serialize(NanobotSwarm.CODEC)
            .sync(NanobotSwarm.STREAM_CODEC)
            ::build));
           static {DATA_COMPONENTS.register("swarm", () -> SWARM);}
    //</editor-fold>

    /// Returns an unmodifiable updating collection of all items from the mod.
    @UnmodifiableView
    public static Collection<DeferredHolder<Item, ? extends Item>> getAllItems() {
        return ITEMS.getEntries();
    }

    // This is my new favourite class. It is usable in both DataComponent and DataAttachment get/set methods.
    public record Swarm(Codec<@Unmodifiable EffectMap>                                 codec,
                        StreamCodec<RegistryFriendlyByteBuf, @Unmodifiable EffectMap>  streamCodec,
                        DeferredHolder<AttachmentType<?>, AttachmentType<NanobotSwarm>>     attachment
    ) implements DataComponentType<@Unmodifiable EffectMap>, Supplier<AttachmentType<NanobotSwarm>> {
        @Override
        public AttachmentType<NanobotSwarm> get() {return attachment.get();}

        @Override
        public String toString() {
            return attachment.getRegisteredName();
        }
    }



    static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        DATA_ATTACHMENTS.register(modBus);
        EFFECT_CODECS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        DATA_COMPONENTS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);

        modBus.addListener((NewRegistryEvent event) -> event.register(EFFECT_CODEC_REG));
        modBus.addListener((DataPackRegistryEvent.NewRegistry event) ->
                event.dataPackRegistry(EFFECTS_KEY, EffectConfiguration.CODEC, EffectConfiguration.CLIENT_CODEC)
        );
    }
}

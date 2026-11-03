package bgps.vanillaplus;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.tag.PreFlattenTagRegistrar;
import io.papermc.paper.tag.TagEntry;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import org.bukkit.block.BlockType;
import org.bukkit.inventory.ItemType;

/**
 * Eski data pack'in etiket (tag) dosyalarinin karsiligi:
 *  - mace_drill:passable / protected / skip  (gurzun tool bileseni #mace_drill:protected'i kullanir)
 *  - minecraft:enchantable/mining ve mining_loot etiketlerine mace eklenir
 */
public final class BGPsBootstrap implements PluginBootstrap {

    private static final String[] PASSABLE_VALUES = {"water", "lava", "bubble_column"};

    private static final String[] PROTECTED_VALUES = {
            "obsidian", "crying_obsidian", "bedrock", "chest", "trapped_chest", "ender_chest", "barrel", "furnace",
            "blast_furnace", "smoker", "hopper", "dropper", "dispenser", "crafter", "brewing_stand", "chiseled_bookshelf",
            "lectern", "jukebox", "decorated_pot", "campfire", "soul_campfire", "spawner", "trial_spawner", "vault",
            "beacon", "copper_chest", "exposed_copper_chest", "weathered_copper_chest", "oxidized_copper_chest",
            "waxed_copper_chest", "waxed_exposed_copper_chest", "waxed_weathered_copper_chest", "waxed_oxidized_copper_chest"
    };
    // protected.json: #shulker_boxes ve #wither_immune "required":false, #beds ve #anvil zorunlu
    private static final String[] PROTECTED_OPTIONAL_TAGS = {"shulker_boxes", "wither_immune"};
    private static final String[] PROTECTED_REQUIRED_TAGS = {"beds", "anvil"};

    @Override
    public void bootstrap(BootstrapContext context) {
        final LifecycleEventManager<BootstrapContext> manager = context.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.BLOCK), event -> {
            final PreFlattenTagRegistrar<BlockType> r = event.registrar();

            final TagKey<BlockType> passable = BlockTypeTagKeys.create(Key.key("mace_drill", "passable"));
            final TagKey<BlockType> protectedTag = BlockTypeTagKeys.create(Key.key("mace_drill", "protected"));
            final TagKey<BlockType> skip = BlockTypeTagKeys.create(Key.key("mace_drill", "skip"));

            final List<TagEntry<BlockType>> passableEntries = new ArrayList<>();
            passableEntries.add(TagEntry.tagEntry(BlockTypeTagKeys.create(Key.key("minecraft", "air")), false));
            for (String v : PASSABLE_VALUES) {
                passableEntries.add(TagEntry.valueEntry(TypedKey.create(RegistryKey.BLOCK, Key.key("minecraft", v)), false));
            }
            r.setTag(passable, passableEntries);

            final List<TagEntry<BlockType>> protectedEntries = new ArrayList<>();
            for (String v : PROTECTED_VALUES) {
                protectedEntries.add(TagEntry.valueEntry(TypedKey.create(RegistryKey.BLOCK, Key.key("minecraft", v)), false));
            }
            for (String t : PROTECTED_OPTIONAL_TAGS) {
                protectedEntries.add(TagEntry.tagEntry(BlockTypeTagKeys.create(Key.key("minecraft", t)), false));
            }
            for (String t : PROTECTED_REQUIRED_TAGS) {
                protectedEntries.add(TagEntry.tagEntry(BlockTypeTagKeys.create(Key.key("minecraft", t))));
            }
            r.setTag(protectedTag, protectedEntries);

            final List<TagEntry<BlockType>> skipEntries = new ArrayList<>();
            skipEntries.add(TagEntry.tagEntry(passable));
            skipEntries.add(TagEntry.tagEntry(protectedTag));
            r.setTag(skip, skipEntries);
        });

        manager.registerEventHandler(LifecycleEvents.TAGS.preFlatten(RegistryKey.ITEM), event -> {
            final PreFlattenTagRegistrar<ItemType> r = event.registrar();
            for (String tag : new String[]{"enchantable/mining", "enchantable/mining_loot"}) {
                final List<TagEntry<ItemType>> mace = new ArrayList<>();
                mace.add(TagEntry.valueEntry(TypedKey.create(RegistryKey.ITEM, Key.key("minecraft", "mace"))));
                r.addToTag(ItemTypeTagKeys.create(Key.key("minecraft", tag)), mace);
            }
        });
    }
}

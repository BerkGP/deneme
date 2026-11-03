package bgps.vanillaplus.port;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

/**
 * mace_drill data pack'inin Java karsiligi: 3x3 kazma, can (hp) sistemi, yenileme (refill), buyu temizleme (strip).
 *
 * Eski sistemde isaretci (marker) varliklari ve ilerleme (advancement) tetikleyicisi ile bulunan "kirilan blok",
 * burada dogrudan BlockBreakEvent ile bulunur; geri kalan kurallar (koruma listesi, dayaniklilik kosulu, duzlem secimi,
 * can dusumu, hasar iadesi, efektler) ayni sirayla uygulanir.
 */
public final class MaceDrill implements Listener {
    private static final int MAX_HP = 3500;
    private static final String[] PROTECTED_NAMES = {
            "obsidian", "crying_obsidian", "bedrock", "chest", "trapped_chest", "ender_chest", "barrel", "furnace",
            "blast_furnace", "smoker", "hopper", "dropper", "dispenser", "crafter", "brewing_stand", "chiseled_bookshelf",
            "lectern", "jukebox", "decorated_pot", "campfire", "soul_campfire", "spawner", "trial_spawner", "vault",
            "beacon", "copper_chest", "exposed_copper_chest", "weathered_copper_chest", "oxidized_copper_chest",
            "waxed_copper_chest", "waxed_exposed_copper_chest", "waxed_weathered_copper_chest", "waxed_oxidized_copper_chest"
    };
    private static final String[] PROTECTED_TAGS = {"shulker_boxes", "wither_immune", "beds", "anvil"};

    private final Plugin plugin;
    private final Set<Material> protectedBlocks = EnumSet.noneOf(Material.class);
    private final List<Tag<Material>> protectedTags = new ArrayList<>();

    public MaceDrill(Plugin plugin) {
        this.plugin = plugin;
        for (String n : PROTECTED_NAMES) {
            Material m = Material.matchMaterial(n);
            if (m != null) protectedBlocks.add(m);
        }
        for (String t : PROTECTED_TAGS) {
            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(t), Material.class);
            if (tag != null) protectedTags.add(tag);
        }
    }

    // ---------- etiket karsiliklari ----------

    /** #mace_drill:passable */
    private static boolean isPassable(Material m) {
        return m.isAir() || m == Material.WATER || m == Material.LAVA || m == Material.BUBBLE_COLUMN;
    }

    /** #mace_drill:protected */
    private boolean isProtected(Material m) {
        if (protectedBlocks.contains(m)) return true;
        for (Tag<Material> t : protectedTags) if (t.isTagged(m)) return true;
        return false;
    }

    // ---------- esya yardimcilari ----------

    /** items entity ... minecraft:mace[minecraft:custom_data={drill:"on"}] (tam eslesme) */
    private static boolean isDrillOn(Map<String, Object> comps) {
        Map<String, Object> cd = Nbt.map(comps.get("minecraft:custom_data"));
        return cd != null && cd.size() == 1 && "on".equals(cd.get("drill"));
    }

    /** minecraft:custom_data~{refill:"yes"} (kismi eslesme) */
    private static boolean isRefill(Map<String, Object> comps) {
        Map<String, Object> cd = Nbt.map(comps.get("minecraft:custom_data"));
        return cd != null && "yes".equals(cd.get("refill"));
    }

    private static Number hp(Map<String, Object> comps) {
        List<Object> f = Nbt.list(Nbt.get(comps, "minecraft:custom_model_data", "floats"));
        if (f == null || f.isEmpty() || !(f.get(0) instanceof Number)) return null;
        return (Number) f.get(0);
    }

    // ---------- her tick ----------

    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                refillScan(p);
                hpCheck(p);
                stripCheck(p);
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.WARNING, "mace_drill tick hatasi (" + p.getName() + ")", e);
            }
        }
    }

    // refill_scan / refill_slot / refill_apply
    private void refillScan(Player p) {
        PlayerInventory inv = p.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack it = inv.getItem(slot);
            if (it != null && it.getType() == Material.MACE) refillSlot(p, slot, it);
        }
        ItemStack off = inv.getItemInOffHand();
        if (off.getType() == Material.MACE) refillSlot(p, -1, off);
    }

    private void refillSlot(Player p, int slot, ItemStack it) {
        Map<String, Object> root = ItemNbt.read(it);
        Map<String, Object> comps = Nbt.components(root);
        if (!isRefill(comps)) return;

        Number cur = hp(comps);
        int n = cur == null ? 0 : cur.intValue();
        n += 500;
        if (n > MAX_HP) n = MAX_HP;

        HpLore.apply(comps, n);                                   // mace_drill:hp/<n>
        Map<String, Object> cd = Nbt.compound();                  // mace_drill:clear_marker
        cd.put("drill", "on");
        comps.put("minecraft:custom_data", cd);

        ItemStack out = ItemNbt.write(root);
        PlayerInventory inv = p.getInventory();
        if (slot < 0) inv.setItemInOffHand(out); else inv.setItem(slot, out);

        p.sendActionBar(Msg.get("refill"));
        p.playSound(p.getLocation(), "minecraft:block.smithing_table.use", SoundCategory.PLAYERS, 1.0F, 1.2F);
    }

    // hp_check
    private void hpCheck(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        if (main.getType() != Material.MACE) return;
        Map<String, Object> root = ItemNbt.read(main);
        Map<String, Object> comps = Nbt.components(root);
        if (!isDrillOn(comps)) return;

        Number cur = hp(comps);
        if (cur == null) {
            HpLore.apply(comps, MAX_HP);
            p.getInventory().setItemInMainHand(ItemNbt.write(root));
            return;
        }
        List<Object> lore = Nbt.list(comps.get("minecraft:lore"));
        if (lore == null || lore.size() < 3) {
            int n = cur.intValue();
            if (n >= 0 && n <= MAX_HP && n % 5 == 0) {
                HpLore.apply(comps, n);
                p.getInventory().setItemInMainHand(ItemNbt.write(root));
            }
        }
    }

    // strip_check / strip_slot
    private void stripCheck(Player p) {
        PlayerInventory inv = p.getInventory();
        boolean stripped = false;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack it = inv.getItem(slot);
            if (strip(it)) { inv.setItem(slot, it); stripped = true; }
        }
        ItemStack off = inv.getItemInOffHand();
        if (strip(off)) { inv.setItemInOffHand(off); stripped = true; }
        if (stripped) p.sendActionBar(Msg.get("strip"));
    }

    private static boolean strip(ItemStack it) {
        if (it == null || it.getType() != Material.MACE) return false;
        if (!(it.containsEnchantment(Enchantment.EFFICIENCY) || it.containsEnchantment(Enchantment.FORTUNE)
                || it.containsEnchantment(Enchantment.SILK_TOUCH))) return false;
        if (isDrillOn(Nbt.components(ItemNbt.read(it)))) return false;
        it.removeEnchantment(Enchantment.EFFICIENCY);
        it.removeEnchantment(Enchantment.FORTUNE);
        it.removeEnchantment(Enchantment.SILK_TOUCH);
        return true;
    }

    // ---------- 3x3 kazma ----------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        GameMode gm = p.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;        // yaratici modda dayaniklilik degismez
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool.getType() != Material.MACE) return;
        if (!isDrillOn(Nbt.components(ItemNbt.read(tool)))) return;

        Block b = e.getBlock();
        if (isProtected(b.getType())) return;                                   // ray_hit: korumali blokta isaretci olusmaz

        final int dmgBefore = ItemNbt.damage(tool);
        final Location center = b.getLocation().add(0.5, 0.5, 0.5);
        final float yaw = p.getLocation().getYaw();
        final float pitch = p.getLocation().getPitch();
        Bukkit.getScheduler().runTask(plugin, () -> finishDrill(p, center, yaw, pitch, dmgBefore));
    }

    // drilled.mcfunction
    private void finishDrill(Player p, Location center, float yaw, float pitch, int dmgBefore) {
        try {
            if (!p.isOnline()) return;
            ItemStack tool = p.getInventory().getItemInMainHand();
            if (tool.getType() != Material.MACE) return;
            if (!isDrillOn(Nbt.components(ItemNbt.read(tool)))) return;

            // item_durability_changed (durability min 12): dayaniklilik degismis ve kalan >= 12 olmali
            int dmg = ItemNbt.damage(tool);
            if (dmg <= dmgBefore) return;
            if (ItemNbt.maxDamage(tool) - dmg < 12) return;

            World w = center.getWorld();
            Block cb = center.getBlock();
            if (!isPassable(cb.getType())) return;                              // hedef blok artik kirilmis (gecilebilir) olmali

            boolean silk = tool.containsEnchantment(Enchantment.SILK_TOUCH);
            int plane = DrillGeometry.plane(yaw, pitch);
            for (int[] o : DrillGeometry.offsets(plane)) {                      // drill_at -> plane_* -> mine_one
                Block t = cb.getRelative(o[0], o[1], o[2]);
                Material m = t.getType();
                if (isPassable(m) || isProtected(m)) continue;                  // #mace_drill:skip
                t.breakNaturally(tool, true, !silk);                            // alet ile dusurme, sessizce deneyim yok (silk)
            }

            w.spawnParticle(Particle.POOF, center, 12, 0.6, 0.6, 0.6, 0.05);
            w.spawnParticle(Particle.CRIT, center, 25, 0.8, 0.8, 0.8, 0.3);
            w.spawnParticle(Particle.ELECTRIC_SPARK, center, 15, 0.7, 0.7, 0.7, 0.1);
            w.playSound(center, "minecraft:item.mace.smash_ground", SoundCategory.BLOCKS, 0.8F, 1.5F);
            w.playSound(center, "minecraft:entity.generic.explode", SoundCategory.BLOCKS, 0.3F, 1.7F);

            useHp(p);
        } catch (RuntimeException ex) {
            plugin.getLogger().log(Level.WARNING, "3x3 kazma hatasi (" + p.getName() + ")", ex);
        }
    }

    // use_hp + apply
    private void useHp(Player p) {
        ItemStack main = p.getInventory().getItemInMainHand();
        if (main.getType() != Material.MACE) return;
        Map<String, Object> root = ItemNbt.read(main);
        Map<String, Object> comps = Nbt.components(root);
        Number cur = hp(comps);
        if (cur == null) return;                                                // #ok: custom_model_data yoksa cik

        int n = cur.intValue() - 5;
        if (n <= 0) n = 0;
        HpLore.apply(comps, n);                                                 // mace_drill:hp/<n>

        int dmg = Nbt.intOf(comps.get("minecraft:damage"));                     // mace_drill:refund
        int refunded = DrillGeometry.refund(dmg, ItemNbt.maxDamage(main));
        if (refunded <= 0) comps.remove("minecraft:damage"); else comps.put("minecraft:damage", Integer.valueOf(refunded));

        p.getInventory().setItemInMainHand(ItemNbt.write(root));
        if (n == 0) p.sendActionBar(Msg.get("use_hp_empty"));
    }
}

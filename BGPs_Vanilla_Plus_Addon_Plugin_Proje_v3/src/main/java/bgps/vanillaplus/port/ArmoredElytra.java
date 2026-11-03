package bgps.vanillaplus.port;

import io.papermc.paper.datacomponent.DataComponentTypes;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

/**
 * armored_elytra data pack'inin Java karsiligi. Eski sistemdeki scoreboard degerleri oyuncu basina bir durum
 * nesnesinde tutulur ve player.mcfunction'daki adimlar ayni sirayla uygulanir:
 * egilme / hasar / ucus algilama, ucus hasarinin elytra canina yazilmasi, tamirin elytraya aktarilmasi,
 * mending deneyiminin elytraya aktarilmasi, gogusluk kirilinca elytra verilmesi.
 */
public final class ArmoredElytra implements Listener {
    private static final String[] CHESTPLATE_NAMES = {
            "leather_chestplate", "chainmail_chestplate", "iron_chestplate", "copper_chestplate",
            "golden_chestplate", "diamond_chestplate", "netherite_chestplate"
    };

    /** Oyuncu basina eski scoreboard degerleri. */
    private static final class St {
        boolean init;
        int sneakl, hurtl, avil;     // ae_sneakl, ae_hurtl, ae_avil
        int avirec;                  // ae_avirec
        int hit;                     // ae_hit
        int last;                    // ae_last
        int valid;                   // ae_valid
        int xplast, skip;            // ae_xplast, ae_skip
        int fsave;                   // ae_fsave
        boolean broke;               // etiket: ae_broke
    }

    /** worn.mcfunction'daki #dmg, #ed, #chg degiskenleri. */
    private static final class Ctx {
        int dmg, ed, chg;
    }

    private final Plugin plugin;
    private final Set<Material> chestplates = EnumSet.noneOf(Material.class);
    private final Map<UUID, St> states = new HashMap<>();
    private final Map<UUID, ItemStack> brk = new HashMap<>();   // storage armored_elytra:brk

    public ArmoredElytra(Plugin plugin) {
        this.plugin = plugin;
        for (String n : CHESTPLATE_NAMES) {
            Material m = Material.matchMaterial(n);
            if (m != null) chestplates.add(m);
        }
    }

    private St state(Player p) {
        return states.computeIfAbsent(p.getUniqueId(), k -> new St());
    }

    /** equipment.chest.components."minecraft:glider" var mi? */
    private static boolean hasGlider(ItemStack it) {
        return it.hasData(DataComponentTypes.GLIDER);
    }

    // ---------- her tick: armored_elytra:tick ----------

    public void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            St st = states.get(p.getUniqueId());
            if (st != null && st.broke) {
                try { breakFinish(p, st); } catch (RuntimeException e) { warn(p, "break_finish", e); }
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { player(p, state(p)); } catch (RuntimeException e) { warn(p, "player", e); }
        }
    }

    private void warn(Player p, String where, RuntimeException e) {
        plugin.getLogger().log(Level.WARNING, "armored_elytra " + where + " hatasi (" + p.getName() + ")", e);
    }

    // ---------- player.mcfunction ----------

    private void player(Player p, St st) {
        int sneakStat = p.getStatistic(Statistic.SNEAK_TIME);
        int hurtStat = p.getStatistic(Statistic.DAMAGE_TAKEN);
        int aviStat = p.getStatistic(Statistic.AVIATE_ONE_CM);
        if (!st.init) {                                   // scoreboard kriteri 0'dan baslar; burada mevcut degerden baslatiyoruz
            st.sneakl = sneakStat;
            st.hurtl = hurtStat;
            st.avil = aviStat;
            st.init = true;
        }

        // egilme
        int sneak = (sneakStat - st.sneakl) >= 1 ? 1 : 0;
        st.sneakl = sneakStat;

        // hasar aldi mi? (damage_taken istatistigi + hit advancement)
        int hurt = (hurtStat - st.hurtl) >= 1 ? 1 : 0;
        st.hurtl = hurtStat;
        if (st.hit >= 1) hurt = 1;
        if (st.hit >= 1) st.hit -= 1;

        // ucuyor mu?
        if (st.avirec >= 1) st.avirec -= 1;
        if (aviStat - st.avil >= 1) st.avirec = 60;
        st.avil = aviStat;

        // zirhli elytra giyiyor mu?
        ItemStack chest = p.getInventory().getChestplate();
        boolean worn = chest != null && chestplates.contains(chest.getType()) && hasGlider(chest);
        if (worn) worn(p, st, chest, sneak, hurt);
        else st.valid = 0;
    }

    // ---------- worn.mcfunction ----------

    private void worn(Player p, St st, ItemStack chest, int sneak, int hurt) {
        Map<String, Object> root = ItemNbt.read(chest);
        Map<String, Object> comps = Nbt.components(root);
        if (!ElytraNbt.hasGlider(comps)) { st.valid = 0; return; }

        Ctx c = new Ctx();
        c.dmg = ElytraNbt.damage(comps);
        c.ed = ElytraNbt.ed(comps);
        c.chg = ElytraNbt.hasEd(comps) ? 0 : 1;           // eski (veri yazilmamis) zirhli elytralari tek seferlik hazirla

        if (st.valid == 1) delta(c, st, sneak, hurt);
        xp(p, c, st, ElytraNbt.hasMending(comps));
        if (c.chg == 1) commit(p, c);
        if (c.ed >= 431) elytraOff(p);

        st.last = c.dmg;
        st.valid = 1;
    }

    // delta.mcfunction (+ flight, repair_to_elytra)
    private void delta(Ctx c, St st, int sneak, int hurt) {
        int delta = c.dmg - st.last;
        // SADECE: tam 1 hasar + oyuncu hasar almamis + oyuncu ucuyor => ucus hasari (elytraya yazilir)
        if (delta == 1 && hurt == 0 && st.avirec >= 1) {
            c.ed += delta;
            c.dmg = st.last;
            c.chg = 1;
        }
        if (delta <= -1 && sneak == 1 && c.ed >= 1) {
            int k = delta * -1;
            int r = Math.min(k, c.ed);
            c.ed -= r;
            c.dmg += r;
            c.chg = 1;
        }
    }

    // xp.mcfunction (+ xp_route, xp_snap, xp_total)
    private void xp(Player p, Ctx c, St st, boolean mending) {
        int points = (int) (p.getExp() * (float) p.getExpToLevel());   // experience query @s points
        int xpt = XpMath.total(p.getLevel(), points);

        if (st.skip == 1) {                                            // xp_snap
            st.skip = 0;
            st.xplast = xpt;
            return;
        }
        if (st.valid == 1 && c.ed >= 1 && mending) {                   // xp_route
            int gain = xpt - st.xplast;
            if (gain >= 1) {
                int r = Math.min(gain * 2, c.ed);
                c.ed -= r;
                int used = Math.floorDiv(r + 1, 2);
                c.chg = 1;
                p.giveExp(-used);                                      // experience add @s -used points
                st.skip = 1;
            }
        }
        st.xplast = xpt;
    }

    // commit.mcfunction + install_item
    private void commit(Player p, Ctx c) {
        ItemStack chest = p.getInventory().getChestplate();
        if (chest == null) return;
        Map<String, Object> root = ItemNbt.read(chest);
        if (c.dmg < 0) c.dmg = 0;
        ElytraNbt.commit(root, c.dmg, c.ed);
        p.getInventory().setChestplate(ItemNbt.write(root));
    }

    // elytra_off.mcfunction + make_elytra + install_item
    private void elytraOff(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack chest = inv.getChestplate();
        if (chest == null) return;
        Map<String, Object> root = ItemNbt.read(chest);
        Map<String, Object> el = ElytraNbt.makeElytra(root, 431);
        ElytraNbt.toPlainChestplate(root);
        inv.setChestplate(ItemNbt.write(root));

        Location loc = p.getLocation();
        World w = loc.getWorld();
        Item drop = w.dropItem(loc, ItemNbt.write(el));
        drop.setPickupDelay(0);
        w.playSound(loc, "minecraft:entity.item.break", SoundCategory.PLAYERS, 1.0F, 0.8F);
        p.sendActionBar(Msg.get("elytra_off"));
    }

    // ---------- gogusluk kirilmasi: chest_break.mcfunction ----------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent e) {
        ItemStack it = e.getItem();
        if (!chestplates.contains(it.getType())) return;
        // item_durability_changed (durability max 0): kalan dayaniklilik 0'a iniyor mu?
        if (ItemNbt.maxDamage(it) - (ItemNbt.damage(it) + e.getDamage()) > 0) return;
        try {
            chestBreak(e.getPlayer());
        } catch (RuntimeException ex) {
            warn(e.getPlayer(), "chest_break", ex);
        }
    }

    private void chestBreak(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack chest = inv.getChestplate();
        if (chest == null || !chestplates.contains(chest.getType())) return;
        if (!hasGlider(chest)) return;

        St st = state(p);
        Map<String, Object> root = ItemNbt.read(chest);
        Map<String, Object> comps = Nbt.components(root);
        if (!ElytraNbt.hasGlider(comps)) return;
        int ed = ElytraNbt.ed(comps);
        st.fsave = 0;

        if (st.avirec >= 1 && st.hit == 0 && ed <= 430) {                // flight_save
            int dmg = ElytraNbt.damage(comps);
            ed += 1;
            ElytraNbt.commit(root, dmg, ed);
            brk.put(p.getUniqueId(), ItemNbt.write(root));
            st.fsave = 1;
            st.broke = true;
            return;
        }
        Map<String, Object> el = ElytraNbt.makeElytra(root, ed);         // make_elytra + chest_save
        brk.put(p.getUniqueId(), ItemNbt.write(el));
        st.broke = true;
    }

    // break_finish.mcfunction
    private void breakFinish(Player p, St st) {
        st.broke = false;
        ItemStack item = brk.remove(p.getUniqueId());
        if (item == null) { st.fsave = 0; return; }
        PlayerInventory inv = p.getInventory();
        ItemStack now = inv.getChestplate();
        if (now != null && now.getType() != Material.AIR) { st.fsave = 0; return; }
        inv.setChestplate(item);
        if (st.fsave == 0) p.sendActionBar(Msg.get("break_finish"));
        st.fsave = 0;
    }

    // ---------- hit.mcfunction (entity_hurt_player) ----------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHurt(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p) state(p).hit = 1;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        St st = states.get(e.getPlayer().getUniqueId());
        if (st != null && !st.broke) states.remove(e.getPlayer().getUniqueId());
    }
}

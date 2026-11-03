package bgps.vanillaplus.port;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** armored_elytra: commit / commit_macro / make_elytra / elytra_off icindeki `data modify` islemleri. */
public final class ElytraNbt {
    private ElytraNbt() {}

    public static final int MAX_ED = 432;

    /** make_elytra.mcfunction'da silinen sadece-zirh buyuleri. */
    public static final String[] ARMOR_ONLY = {
            "minecraft:protection", "minecraft:fire_protection", "minecraft:blast_protection",
            "minecraft:projectile_protection", "minecraft:thorns", "minecraft:respiration",
            "minecraft:aqua_affinity", "minecraft:depth_strider", "minecraft:frost_walker",
            "minecraft:soul_speed", "minecraft:swift_sneak", "minecraft:feather_falling"
    };

    private static Map<String, Object> cfg;

    private static synchronized Map<String, Object> cfg() {
        if (cfg == null) cfg = Nbt.map(JsonNbt.convert(Json.parse(Res.text("/port/elytra.json"))));
        return cfg;
    }

    // ---------- okuma ----------

    public static int damage(Map<String, Object> comps) {
        return Nbt.intOf(comps.get("minecraft:damage"));
    }

    public static boolean hasEd(Map<String, Object> comps) {
        Map<String, Object> cd = Nbt.map(comps.get("minecraft:custom_data"));
        return cd != null && cd.get("ae_ed") instanceof Number;
    }

    public static int ed(Map<String, Object> comps) {
        Map<String, Object> cd = Nbt.map(comps.get("minecraft:custom_data"));
        return cd == null ? 0 : Nbt.intOf(cd.get("ae_ed"));
    }

    public static boolean hasMending(Map<String, Object> comps) {
        Map<String, Object> e = Nbt.map(comps.get("minecraft:enchantments"));
        return e != null && e.containsKey("minecraft:mending");
    }

    public static boolean hasGlider(Map<String, Object> comps) {
        return comps.containsKey("minecraft:glider");
    }

    // ---------- yazma ----------

    private static Map<String, Object> text(String text, String color) {
        Map<String, Object> m = Nbt.compound();
        m.put("text", text);
        m.put("color", color);
        return m;
    }

    /** damage bilesenini yazar. 0 ise varsayilan deger oldugu icin bileseni kaldirir. */
    public static void setDamage(Map<String, Object> comps, int dmg) {
        if (dmg <= 0) comps.remove("minecraft:damage");
        else comps.put("minecraft:damage", Integer.valueOf(dmg));
    }

    /**
     * commit.mcfunction + commit_macro.mcfunction: hasar, ae_ed, "Elytra Cani" lore satiri (3. satir)
     * ve sessiz equip sesi yazilir.
     */
    public static void commit(Map<String, Object> root, int dmg, int ed) {
        Map<String, Object> c = cfg();
        Map<String, Object> comps = Nbt.components(root);

        // equippable.equip_sound
        Map<String, Object> snd = Nbt.compound();
        snd.put("sound_id", c.get("silent_sound"));
        Nbt.put(root, snd, "components", "minecraft:equippable", "equip_sound");

        // damage
        setDamage(comps, dmg);

        // custom_data.ae_ed
        Nbt.put(root, Integer.valueOf(ed), "components", "minecraft:custom_data", "ae_ed");

        // lore[2]
        int hp = MAX_ED - ed;
        List<Object> lore = Nbt.list(comps.get("minecraft:lore"));
        if (lore == null) { lore = new ArrayList<>(); comps.put("minecraft:lore", lore); }
        if (lore.size() < 3) {
            Map<String, Object> empty = Nbt.compound();
            empty.put("text", "");
            lore.add(empty);
        }
        if (lore.size() >= 3) {
            List<Object> extra = new ArrayList<>();
            extra.add(text((String) c.get("label"), (String) c.get("label_color")));
            extra.add(text(Integer.toString(hp), (String) c.get("num_color")));
            extra.add(text((String) c.get("suffix"), (String) c.get("suffix_color")));
            Map<String, Object> line = Nbt.compound();
            line.put("text", "");
            line.put("italic", Byte.valueOf((byte) 0));
            line.put("extra", extra);
            lore.set(2, line);
        }
    }

    /** make_elytra.mcfunction: gogusluktan, sadece-zirh buyulerinden arindirilmis bir elytra olusturur. */
    public static Map<String, Object> makeElytra(Map<String, Object> chestRoot, int damage) {
        Map<String, Object> el = Nbt.compound();
        el.put("id", "minecraft:elytra");
        el.put("count", Integer.valueOf(1));
        Map<String, Object> ec = Nbt.compound();
        el.put("components", ec);

        Map<String, Object> comps = Nbt.map(chestRoot.get("components"));
        Map<String, Object> ench = comps == null ? null : Nbt.map(comps.get("minecraft:enchantments"));
        if (ench != null) {
            Map<String, Object> copy = Nbt.map(Nbt.deepCopy(ench));
            for (String k : ARMOR_ONLY) copy.remove(k);
            ec.put("minecraft:enchantments", copy);
        }
        if (damage != 0) ec.put("minecraft:damage", Integer.valueOf(damage));
        if (chestRoot.containsKey("DataVersion")) el.put("DataVersion", chestRoot.get("DataVersion"));
        return el;
    }

    /** elytra_off: glider, equippable, lore ve custom_data bilesenleri silinir; gogusluk sade haline doner. */
    public static void toPlainChestplate(Map<String, Object> root) {
        Map<String, Object> comps = Nbt.map(root.get("components"));
        if (comps == null) return;
        comps.remove("minecraft:glider");
        comps.remove("minecraft:equippable");
        comps.remove("minecraft:lore");
        comps.remove("minecraft:custom_data");
    }
}

package bgps.vanillaplus.port;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.plugin.Plugin;

/**
 * Data pack tariflerini (resources/port/recipes/**.json, data pack'ten birebir kopya) Bukkit tarifi olarak kaydeder.
 * Tarif anahtarlari eski data pack ile ayni kalir (mace_drill:..., armored_elytra:...).
 */
public final class Recipes {
    private Recipes() {}

    public static final String[][] FILES = {
            {"mace_drill", "rainbow_template"}, {"mace_drill", "drill_book"},
            {"mace_drill", "mace_drill"}, {"mace_drill", "mace_refill"},
            {"armored_elytra", "leather_chestplate_glider"}, {"armored_elytra", "chainmail_chestplate_glider"},
            {"armored_elytra", "iron_chestplate_glider"}, {"armored_elytra", "copper_chestplate_glider"},
            {"armored_elytra", "golden_chestplate_glider"}, {"armored_elytra", "diamond_chestplate_glider"},
            {"armored_elytra", "netherite_chestplate_glider"}
    };

    private static Map<String, Object> load(String ns, String name) {
        return Nbt.map(Json.parse(Res.text("/port/recipes/" + ns + "/" + name + ".json")));
    }

    /** Tarifin sonuc esyasi. */
    public static ItemStack result(String ns, String name) {
        return ItemNbt.fromJsonItem(Nbt.map(load(ns, name).get("result")));
    }

    private static RecipeChoice choice(Object id) {
        Material m = Material.matchMaterial((String) id);
        if (m == null) throw new IllegalStateException("Bilinmeyen esya: " + id);
        return new RecipeChoice.MaterialChoice(m);
    }

    public static void registerAll(Plugin plugin) {
        int ok = 0;
        for (String[] f : FILES) {
            try {
                register(f[0], f[1]);
                ok++;
            } catch (RuntimeException e) {
                plugin.getLogger().log(Level.SEVERE, "Tarif yuklenemedi: " + f[0] + ":" + f[1], e);
            }
        }
        plugin.getLogger().info(ok + "/" + FILES.length + " tarif kaydedildi.");
    }

    public static void unregisterAll() {
        for (String[] f : FILES) {
            NamespacedKey key = NamespacedKey.fromString(f[0] + ":" + f[1]);
            if (key != null) Bukkit.removeRecipe(key);
        }
    }

    private static void register(String ns, String name) {
        Map<String, Object> j = load(ns, name);
        String type = (String) j.get("type");
        NamespacedKey key = NamespacedKey.fromString(ns + ":" + name);
        if (key == null) throw new IllegalStateException("Gecersiz anahtar: " + ns + ":" + name);
        ItemStack result = ItemNbt.fromJsonItem(Nbt.map(j.get("result")));

        Recipe recipe;
        if ("minecraft:crafting_shaped".equals(type)) {
            ShapedRecipe r = new ShapedRecipe(key, result);
            List<Object> pat = Nbt.list(j.get("pattern"));
            String[] shape = new String[pat.size()];
            for (int i = 0; i < shape.length; i++) shape[i] = (String) pat.get(i);
            r.shape(shape);
            for (Map.Entry<String, Object> e : Nbt.map(j.get("key")).entrySet()) {
                Material m = Material.matchMaterial((String) e.getValue());
                if (m == null) throw new IllegalStateException("Bilinmeyen esya: " + e.getValue());
                r.setIngredient(e.getKey().charAt(0), m);
            }
            recipe = r;
        } else if ("minecraft:smithing_transform".equals(type)) {
            recipe = new SmithingTransformRecipe(key, result, choice(j.get("template")), choice(j.get("base")), choice(j.get("addition")));
        } else {
            throw new IllegalStateException("Desteklenmeyen tarif turu: " + type);
        }
        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
    }
}

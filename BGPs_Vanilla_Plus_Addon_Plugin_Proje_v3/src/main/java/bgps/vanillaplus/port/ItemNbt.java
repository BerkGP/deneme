package bgps.vanillaplus.port;

import java.io.IOException;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

/** ItemStack <-> NBT koprusu. Data pack'in `data modify ... item.components` islemleri bu haritalar uzerinde yapilir. */
public final class ItemNbt {
    private ItemNbt() {}

    private static volatile Integer dataVersion;

    public static Map<String, Object> read(ItemStack item) {
        try {
            return Nbt.readRoot(item.serializeAsBytes());
        } catch (IOException e) {
            throw new IllegalStateException("Esya NBT'si okunamadi", e);
        }
    }

    public static ItemStack write(Map<String, Object> root) {
        try {
            return ItemStack.deserializeBytes(Nbt.writeRoot(root));
        } catch (IOException e) {
            throw new IllegalStateException("Esya NBT'si yazilamadi", e);
        }
    }

    /** Sunucunun gecerli DataVersion degeri (veri donusumu gerekmesin diye). */
    public static int dataVersion() {
        Integer v = dataVersion;
        if (v == null) {
            Object o = read(new ItemStack(Material.STONE)).get("DataVersion");
            v = Integer.valueOf(Nbt.intOf(o));
            dataVersion = v;
        }
        return v.intValue();
    }

    /** Tarif JSON'undaki "result" nesnesinden ({id, count, components}) esya olusturur. */
    public static ItemStack fromJsonItem(Map<String, Object> jsonItem) {
        Map<String, Object> root = Nbt.map(JsonNbt.convert(jsonItem));
        root.put("DataVersion", Integer.valueOf(dataVersion()));
        return write(root);
    }

    public static int damage(ItemStack item) {
        ItemMeta m = item.getItemMeta();
        return m instanceof Damageable d ? d.getDamage() : 0;
    }

    public static int maxDamage(ItemStack item) {
        ItemMeta m = item.getItemMeta();
        if (m instanceof Damageable d && d.hasMaxDamage()) return d.getMaxDamage();
        return item.getType().getMaxDurability();
    }
}

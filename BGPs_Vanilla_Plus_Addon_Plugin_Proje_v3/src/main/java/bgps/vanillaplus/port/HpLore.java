package bgps.vanillaplus.port;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * mace_drill:hp/<n> item modifier'larinin Java karsiligi.
 * Data pack'te 701 ayri dosya (0, 5, 10 ... 3500) olan bu modifier'lar tek bir kalip uzerinden uretilir.
 * n == 0 ise ek olarak custom_data {drill:"off"} ve devre disi tool bileseni yazilir.
 */
public final class HpLore {
    private HpLore() {}

    private static Map<String, Object> cfg;

    private static synchronized Map<String, Object> cfg() {
        if (cfg == null) cfg = Nbt.map(JsonNbt.convert(Json.parse(Res.text("/port/hp.json"))));
        return cfg;
    }

    /** Sadece testler icin: baska bir hp.json iceregi yukler. */
    public static synchronized void reset() { cfg = null; }

    private static Map<String, Object> text(String text, String color) {
        Map<String, Object> m = Nbt.compound();
        m.put("text", text);
        m.put("color", color);
        return m;
    }

    private static Map<String, Object> thirdLine(int n, Map<String, Object> c) {
        List<Object> extra = new ArrayList<>();
        extra.add(text((String) c.get("label"), (String) c.get("label_color")));
        if (n == 0) {
            extra.add(text((String) c.get("empty"), (String) c.get("empty_color")));
        } else {
            extra.add(text(Integer.toString(n), (String) c.get("num_color")));
            extra.add(text((String) c.get("suffix"), (String) c.get("suffix_color")));
        }
        Map<String, Object> line = Nbt.compound();
        line.put("text", "");
        line.put("italic", Byte.valueOf((byte) 0));
        line.put("extra", extra);
        return line;
    }

    /** hp/<n> modifier'ini bilesen haritasina uygular (set_components: ilgili bilesenleri komple degistirir). */
    public static void apply(Map<String, Object> comps, int n) {
        Map<String, Object> c = cfg();

        Map<String, Object> cmd = Nbt.compound();
        List<Object> floats = new ArrayList<>();
        floats.add(Float.valueOf((float) n));
        cmd.put("floats", floats);
        comps.put("minecraft:custom_model_data", cmd);

        List<Object> lore = new ArrayList<>();
        lore.add(Nbt.deepCopy(c.get("lore0")));
        lore.add(Nbt.deepCopy(c.get("lore1")));
        lore.add(thirdLine(n, c));
        comps.put("minecraft:lore", lore);

        if (n == 0) {
            comps.put("minecraft:custom_data", Nbt.deepCopy(c.get("custom_data_zero")));
            comps.put("minecraft:tool", Nbt.deepCopy(c.get("tool_zero")));
        }
    }
}

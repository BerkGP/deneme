package bgps.vanillaplus.port;

import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/** messages.json'daki action bar metinleri (data pack'teki `title @s actionbar` komutlarindan alinmistir). */
public final class Msg {
    private Msg() {}

    private static Map<String, Object> data;

    private static synchronized Map<String, Object> data() {
        if (data == null) data = Nbt.map(Json.parse(Res.text("/port/messages.json")));
        return data;
    }

    public static Component get(String key) {
        Map<String, Object> m = Nbt.map(data().get(key));
        String color = (String) m.get("color");
        NamedTextColor c = color == null ? null : NamedTextColor.NAMES.value(color);
        return c == null ? Component.text((String) m.get("text")) : Component.text((String) m.get("text"), c);
    }
}

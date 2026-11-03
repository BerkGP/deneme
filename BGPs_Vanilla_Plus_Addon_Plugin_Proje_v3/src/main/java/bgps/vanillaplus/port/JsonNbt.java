package bgps.vanillaplus.port;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Data pack JSON degerlerini NBT tiplerine cevirir (tam sayi -> int, ondalik -> float, bool -> byte). */
public final class JsonNbt {
    private JsonNbt() {}

    @SuppressWarnings("unchecked")
    public static Object convert(Object j) {
        if (j instanceof Map) {
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) j).entrySet()) out.put(e.getKey(), convert(e.getValue()));
            return out;
        }
        if (j instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object e : (List<Object>) j) out.add(convert(e));
            return out;
        }
        if (j instanceof Boolean) return Byte.valueOf((byte) (((Boolean) j) ? 1 : 0));
        if (j instanceof Long) {
            long l = (Long) j;
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) return Integer.valueOf((int) l);
            return Long.valueOf(l);
        }
        if (j instanceof Double) return Float.valueOf((float) ((Double) j).doubleValue());
        return j;
    }
}

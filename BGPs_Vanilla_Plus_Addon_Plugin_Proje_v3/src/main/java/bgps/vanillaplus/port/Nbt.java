package bgps.vanillaplus.port;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Minimal NBT okuyucu/yazici. Tipler Java nesneleri ile temsil edilir:
 * Byte, Short, Integer, Long, Float, Double, String, byte[], int[], long[],
 * List (NBT listesi), Map (NBT compound, sirayi korur).
 */
public final class Nbt {
    private Nbt() {}

    private static final int END = 0, BYTE = 1, SHORT = 2, INT = 3, LONG = 4, FLOAT = 5, DOUBLE = 6,
            BYTE_ARRAY = 7, STRING = 8, LIST = 9, COMPOUND = 10, INT_ARRAY = 11, LONG_ARRAY = 12;

    // ---------- okuma / yazma ----------

    public static Map<String, Object> readRoot(byte[] data) throws IOException {
        InputStream in = new ByteArrayInputStream(data);
        if (data.length > 2 && (data[0] & 0xff) == 0x1f && (data[1] & 0xff) == 0x8b) {
            in = new GZIPInputStream(in);
        }
        try (DataInputStream d = new DataInputStream(new BufferedInputStream(in))) {
            int t = d.readUnsignedByte();
            if (t != COMPOUND) throw new IOException("NBT kok etiketi compound degil: " + t);
            d.readUTF();
            return map(readPayload(d, COMPOUND));
        }
    }

    public static byte[] writeRoot(Map<String, Object> root) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (DataOutputStream d = new DataOutputStream(new GZIPOutputStream(bo))) {
            d.writeByte(COMPOUND);
            d.writeUTF("");
            writePayload(d, root);
        }
        return bo.toByteArray();
    }

    private static Object readPayload(DataInputStream d, int type) throws IOException {
        switch (type) {
            case BYTE: return Byte.valueOf(d.readByte());
            case SHORT: return Short.valueOf(d.readShort());
            case INT: return Integer.valueOf(d.readInt());
            case LONG: return Long.valueOf(d.readLong());
            case FLOAT: return Float.valueOf(d.readFloat());
            case DOUBLE: return Double.valueOf(d.readDouble());
            case BYTE_ARRAY: { byte[] a = new byte[d.readInt()]; d.readFully(a); return a; }
            case STRING: return d.readUTF();
            case LIST: {
                int et = d.readUnsignedByte();
                int n = d.readInt();
                List<Object> l = new ArrayList<>(Math.max(n, 0));
                for (int k = 0; k < n; k++) l.add(readPayload(d, et));
                return l;
            }
            case COMPOUND: {
                Map<String, Object> m = new LinkedHashMap<>();
                while (true) {
                    int t = d.readUnsignedByte();
                    if (t == END) return m;
                    String name = d.readUTF();
                    m.put(name, readPayload(d, t));
                }
            }
            case INT_ARRAY: { int[] a = new int[d.readInt()]; for (int k = 0; k < a.length; k++) a[k] = d.readInt(); return a; }
            case LONG_ARRAY: { long[] a = new long[d.readInt()]; for (int k = 0; k < a.length; k++) a[k] = d.readLong(); return a; }
            default: throw new IOException("Bilinmeyen NBT tipi: " + type);
        }
    }

    private static int typeOf(Object o) {
        if (o instanceof Byte || o instanceof Boolean) return BYTE;
        if (o instanceof Short) return SHORT;
        if (o instanceof Integer) return INT;
        if (o instanceof Long) return LONG;
        if (o instanceof Float) return FLOAT;
        if (o instanceof Double) return DOUBLE;
        if (o instanceof byte[]) return BYTE_ARRAY;
        if (o instanceof String) return STRING;
        if (o instanceof List) return LIST;
        if (o instanceof Map) return COMPOUND;
        if (o instanceof int[]) return INT_ARRAY;
        if (o instanceof long[]) return LONG_ARRAY;
        throw new IllegalArgumentException("NBT'ye cevrilemeyen tip: " + (o == null ? "null" : o.getClass()));
    }

    @SuppressWarnings("unchecked")
    private static void writePayload(DataOutputStream d, Object o) throws IOException {
        switch (typeOf(o)) {
            case BYTE: d.writeByte(o instanceof Boolean ? (((Boolean) o) ? 1 : 0) : ((Byte) o).intValue()); break;
            case SHORT: d.writeShort((Short) o); break;
            case INT: d.writeInt((Integer) o); break;
            case LONG: d.writeLong((Long) o); break;
            case FLOAT: d.writeFloat((Float) o); break;
            case DOUBLE: d.writeDouble((Double) o); break;
            case BYTE_ARRAY: { byte[] a = (byte[]) o; d.writeInt(a.length); d.write(a); break; }
            case STRING: d.writeUTF((String) o); break;
            case LIST: {
                List<Object> l = (List<Object>) o;
                if (l.isEmpty()) { d.writeByte(END); d.writeInt(0); break; }
                int et = typeOf(l.get(0));
                d.writeByte(et);
                d.writeInt(l.size());
                for (Object e : l) {
                    if (typeOf(e) != et) throw new IllegalArgumentException("NBT listesi tek tipli olmali");
                    writePayload(d, e);
                }
                break;
            }
            case COMPOUND: {
                for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                    d.writeByte(typeOf(e.getValue()));
                    d.writeUTF(e.getKey());
                    writePayload(d, e.getValue());
                }
                d.writeByte(END);
                break;
            }
            case INT_ARRAY: { int[] a = (int[]) o; d.writeInt(a.length); for (int v : a) d.writeInt(v); break; }
            case LONG_ARRAY: { long[] a = (long[]) o; d.writeInt(a.length); for (long v : a) d.writeLong(v); break; }
            default: throw new IllegalStateException();
        }
    }

    // ---------- yardimcilar ----------

    public static Map<String, Object> compound() { return new LinkedHashMap<>(); }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> list(Object o) {
        return o instanceof List ? (List<Object>) o : null;
    }

    /** Sayisal etiketi int'e cevirir (data get ... komutundaki gibi kesilerek); yoksa 0. */
    public static int intOf(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    public static Object deepCopy(Object o) {
        if (o instanceof Map) {
            Map<String, Object> c = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) c.put(e.getKey(), deepCopy(e.getValue()));
            return c;
        }
        if (o instanceof List) {
            List<Object> c = new ArrayList<>();
            for (Object e : (List<Object>) o) c.add(deepCopy(e));
            return c;
        }
        if (o instanceof byte[]) return ((byte[]) o).clone();
        if (o instanceof int[]) return ((int[]) o).clone();
        if (o instanceof long[]) return ((long[]) o).clone();
        return o;
    }

    /** m.a.b.c yolundaki degeri verir; yol yoksa null. */
    public static Object get(Map<String, Object> m, String... path) {
        Object cur = m;
        for (String p : path) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<?, ?>) cur).get(p);
        }
        return cur;
    }

    /** Yoldaki ara compound'lari olusturarak degeri yazar (data modify ... set value gibi). */
    public static void put(Map<String, Object> m, Object value, String... path) {
        Map<String, Object> cur = m;
        for (int k = 0; k < path.length - 1; k++) {
            Map<String, Object> next = map(cur.get(path[k]));
            if (next == null) { next = compound(); cur.put(path[k], next); }
            cur = next;
        }
        cur.put(path[path.length - 1], value);
    }

    public static Map<String, Object> components(Map<String, Object> root) {
        Map<String, Object> c = map(root.get("components"));
        if (c == null) { c = compound(); root.put("components", c); }
        return c;
    }
}

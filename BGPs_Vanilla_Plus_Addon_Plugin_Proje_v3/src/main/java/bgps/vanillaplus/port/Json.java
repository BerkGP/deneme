package bgps.vanillaplus.port;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kucuk JSON ayristirici. Nesne -> LinkedHashMap, dizi -> ArrayList, sayi -> Long (tam) / Double (ondalik),
 * metin -> String, true/false -> Boolean, null -> null.
 */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        Json j = new Json(text);
        j.ws();
        Object v = j.value();
        j.ws();
        if (j.i != j.s.length()) throw new IllegalArgumentException("Json: fazla veri, konum " + j.i);
        return v;
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private Object value() {
        if (i >= s.length()) throw new IllegalArgumentException("Json: beklenmeyen son");
        char c = s.charAt(i);
        if (c == '{') return obj();
        if (c == '[') return arr();
        if (c == '"') return str();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        return num();
    }

    private Map<String, Object> obj() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++; ws();
        if (s.charAt(i) == '}') { i++; return m; }
        while (true) {
            ws();
            String k = str();
            ws();
            if (s.charAt(i) != ':') throw new IllegalArgumentException("Json: ':' bekleniyor, konum " + i);
            i++; ws();
            m.put(k, value());
            ws();
            char c = s.charAt(i++);
            if (c == '}') return m;
            if (c != ',') throw new IllegalArgumentException("Json: ',' veya '}' bekleniyor, konum " + (i - 1));
        }
    }

    private List<Object> arr() {
        List<Object> l = new ArrayList<>();
        i++; ws();
        if (s.charAt(i) == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            char c = s.charAt(i++);
            if (c == ']') return l;
            if (c != ',') throw new IllegalArgumentException("Json: ',' veya ']' bekleniyor, konum " + (i - 1));
        }
    }

    private String str() {
        if (s.charAt(i) != '"') throw new IllegalArgumentException("Json: '\"' bekleniyor, konum " + i);
        i++;
        StringBuilder b = new StringBuilder();
        while (true) {
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c != '\\') { b.append(c); continue; }
            char e = s.charAt(i++);
            switch (e) {
                case '"': b.append('"'); break;
                case '\\': b.append('\\'); break;
                case '/': b.append('/'); break;
                case 'b': b.append('\b'); break;
                case 'f': b.append('\f'); break;
                case 'n': b.append('\n'); break;
                case 'r': b.append('\r'); break;
                case 't': b.append('\t'); break;
                case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                default: throw new IllegalArgumentException("Json: gecersiz kacis \\" + e);
            }
        }
    }

    private Object num() {
        int st = i;
        boolean dec = false;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '.' || c == 'e' || c == 'E') dec = true;
            else if (!(c == '-' || c == '+' || (c >= '0' && c <= '9'))) break;
            i++;
        }
        String t = s.substring(st, i);
        if (t.isEmpty()) throw new IllegalArgumentException("Json: gecersiz deger, konum " + st);
        return dec ? (Object) Double.valueOf(t) : (Object) Long.valueOf(t);
    }
}

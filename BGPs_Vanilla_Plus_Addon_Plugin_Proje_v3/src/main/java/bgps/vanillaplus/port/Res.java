package bgps.vanillaplus.port;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Jar icindeki UTF-8 kaynaklari okur. */
public final class Res {
    private Res() {}

    public static String text(String path) {
        try (InputStream in = Res.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Kaynak bulunamadi: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Kaynak okunamadi: " + path, e);
        }
    }
}

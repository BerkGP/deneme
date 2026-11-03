package bgps.vanillaplus.port;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Cekirdek siniflarin (Bukkit'e bagli olmayan kisim) eski data pack ile birebir ayni sonucu verdigini dogrular.
 * Kullanim: java -cp <siniflar>:<kaynaklar> bgps.vanillaplus.port.SelfTest legacy/BGPs_Vanilla_Plus_Addon_DataPack_v3.zip
 */
public final class SelfTest {
    private static int ok = 0, fail = 0;

    private static void check(boolean cond, String what) {
        if (cond) ok++;
        else { fail++; System.out.println("HATA: " + what); }
    }

    private static String entry(ZipFile z, String name) throws IOException {
        ZipEntry e = z.getEntry(name);
        if (e == null) throw new IOException("zip icinde yok: " + name);
        return new String(z.getInputStream(e).readAllBytes(), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        try (ZipFile z = new ZipFile(args[0])) {
            testHp(z);
            testPlanes(z);
            testRecipes(z);
        }
        testAngles();
        testXp();
        testNbtRoundTrip();
        testElytra();
        testRefund();
        System.out.println("TAMAM: " + ok + " kontrol gecti, " + fail + " basarisiz");
        if (fail > 0) System.exit(1);
    }

    // 701 hp modifier dosyasi: uretilen bilesenler dosyadakiyle ayni mi?
    @SuppressWarnings("unchecked")
    private static void testHp(ZipFile z) throws IOException {
        int n = 0;
        for (int hp = 0; hp <= 3500; hp += 5) {
            Map<String, Object> file = Nbt.map(JsonNbt.convert(Json.parse(entry(z, "data/mace_drill/item_modifier/hp/" + hp + ".json"))));
            Map<String, Object> expected = Nbt.map(file.get("components"));
            Map<String, Object> mine = Nbt.compound();
            HpLore.apply(mine, hp);
            check(expected.equals(mine), "hp/" + hp + " bilesenleri farkli");
            n++;
        }
        check(n == 701, "701 hp dosyasi test edilmeli, test edilen: " + n);
        System.out.println("hp modifier: " + n + " dosya karsilastirildi");
    }

    // plane_x/y/z fonksiyonlari: ofset sirasi birebir ayni mi?
    private static void testPlanes(ZipFile z) throws IOException {
        Pattern p = Pattern.compile("positioned ~(-?\\d+) ~(-?\\d+) ~(-?\\d+) unless block ~ ~ ~ #mace_drill:skip run function mace_drill:mine_one");
        String[] names = {"plane_y", "plane_x", "plane_z"};
        int[] ids = {DrillGeometry.PLANE_Y, DrillGeometry.PLANE_X, DrillGeometry.PLANE_Z};
        for (int k = 0; k < 3; k++) {
            List<int[]> exp = new ArrayList<>();
            for (String line : entry(z, "data/mace_drill/function/" + names[k] + ".mcfunction").split("\n")) {
                Matcher m = p.matcher(line);
                if (m.find()) exp.add(new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))});
            }
            int[][] mine = DrillGeometry.offsets(ids[k]);
            check(exp.size() == 9 && mine.length == 9, names[k] + ": 9 satir olmali");
            for (int i = 0; i < 9; i++) check(java.util.Arrays.equals(exp.get(i), mine[i]), names[k] + " satir " + i + " farkli");
        }
        System.out.println("duzlem ofsetleri: 3 fonksiyon x 9 satir karsilastirildi");
    }

    // tarif JSON'lari ayristirilir ve NBT'ye cevrilir
    private static void testRecipes(ZipFile z) throws IOException {
        String[][] rs = {
                {"mace_drill", "rainbow_template"}, {"mace_drill", "drill_book"}, {"mace_drill", "mace_drill"}, {"mace_drill", "mace_refill"},
                {"armored_elytra", "leather_chestplate_glider"}, {"armored_elytra", "chainmail_chestplate_glider"},
                {"armored_elytra", "iron_chestplate_glider"}, {"armored_elytra", "copper_chestplate_glider"},
                {"armored_elytra", "golden_chestplate_glider"}, {"armored_elytra", "diamond_chestplate_glider"},
                {"armored_elytra", "netherite_chestplate_glider"}};
        for (String[] r : rs) {
            String zipText = entry(z, "data/" + r[0] + "/recipe/" + r[1] + ".json");
            String resText = Res.text("/port/recipes/" + r[0] + "/" + r[1] + ".json");
            check(zipText.equals(resText), "tarif kaynagi data pack ile ayni degil: " + r[1]);
            Map<String, Object> j = Nbt.map(Json.parse(resText));
            Map<String, Object> result = Nbt.map(j.get("result"));
            check(result != null && result.get("id") instanceof String, "tarif sonucu okunamadi: " + r[1]);
        }
        System.out.println("tarifler: " + rs.length + " dosya data pack ile birebir ayni");
    }

    private static void testAngles() {
        check(DrillGeometry.plane(0f, 0f) == DrillGeometry.PLANE_Z, "yaw 0 -> z duzlemi");
        check(DrillGeometry.plane(90f, 0f) == DrillGeometry.PLANE_X, "yaw 90 -> x duzlemi");
        check(DrillGeometry.plane(-90f, 10f) == DrillGeometry.PLANE_X, "yaw -90 -> x duzlemi");
        check(DrillGeometry.plane(180f, 0f) == DrillGeometry.PLANE_Z, "yaw 180 -> z duzlemi");
        check(DrillGeometry.plane(45f, 0f) == DrillGeometry.PLANE_X, "yaw 45 (dahil) -> x");
        check(DrillGeometry.plane(135f, 0f) == DrillGeometry.PLANE_X, "yaw 135 (dahil) -> x");
        check(DrillGeometry.plane(136f, 0f) == DrillGeometry.PLANE_Z, "yaw 136 -> z");
        check(DrillGeometry.plane(450f, 0f) == DrillGeometry.PLANE_X, "yaw 450 == 90 -> x");
        check(DrillGeometry.plane(-270f, 0f) == DrillGeometry.PLANE_X, "yaw -270 == 90 -> x");
        check(DrillGeometry.plane(90f, 60f) == DrillGeometry.PLANE_Y, "pitch 60 yaw'i gecersiz kilar -> y");
        check(DrillGeometry.plane(0f, -45f) == DrillGeometry.PLANE_Y, "pitch -45 (dahil) -> y");
        check(DrillGeometry.plane(0f, 44.9f) == DrillGeometry.PLANE_Z, "pitch 44.9 -> z");
        System.out.println("yon secimi: kontroller gecti");
    }

    private static void testXp() {
        check(XpMath.total(0, 0) == 0, "xp seviye 0");
        check(XpMath.total(16, 0) == 352, "xp seviye 16 = 352");
        check(XpMath.total(17, 0) == 394, "xp seviye 17 = 394");
        check(XpMath.total(30, 0) == 1395, "xp seviye 30 = 1395");
        check(XpMath.total(31, 0) == 1507, "xp seviye 31 = 1507");
        check(XpMath.total(32, 0) == 1628, "xp seviye 32 = 1628");
        check(XpMath.total(50, 0) == 5345, "xp seviye 50 = 5345");
        check(XpMath.total(5, 3) == 58, "xp seviye 5 + 3 puan");
        for (int l = 0; l < 100; l++) {
            int need = l >= 31 ? 9 * l - 158 : (l >= 15 ? 5 * l - 38 : 2 * l + 7);
            check(XpMath.total(l + 1, 0) - XpMath.total(l, 0) == need, "xp seviye " + l + " -> " + (l + 1) + " farki");
        }
        System.out.println("xp matematigi: kontroller gecti");
    }

    private static void testNbtRoundTrip() throws IOException {
        Map<String, Object> item = Nbt.compound();
        item.put("id", "minecraft:mace");
        item.put("count", Integer.valueOf(1));
        item.put("DataVersion", Integer.valueOf(4900));
        Map<String, Object> comps = Nbt.components(item);
        HpLore.apply(comps, 3500);
        Nbt.put(item, "on", "components", "minecraft:custom_data", "drill");
        comps.put("minecraft:damage", Integer.valueOf(12));
        comps.put("minecraft:glider", Nbt.compound());
        byte[] bytes = Nbt.writeRoot(item);
        Map<String, Object> back = Nbt.readRoot(bytes);
        check(item.equals(back), "NBT yaz/oku gidis-donus ayni olmali");
        Map<String, Object> tool = Nbt.map(Nbt.get(back, "components", "minecraft:tool"));
        check(tool == null, "hp 3500 icin tool yazilmamali");
        System.out.println("NBT gidis-donus: tamam (" + bytes.length + " bayt)");
    }

    @SuppressWarnings("unchecked")
    private static void testElytra() throws IOException {
        // tarif sonucundaki baslangic lore'u == commit(ed=0) cikisi
        Map<String, Object> j = Nbt.map(Json.parse(Res.text("/port/recipes/armored_elytra/diamond_chestplate_glider.json")));
        Map<String, Object> root = Nbt.map(JsonNbt.convert(j.get("result")));
        root.put("DataVersion", Integer.valueOf(4900));
        Map<String, Object> before = Nbt.map(Nbt.deepCopy(root));
        ElytraNbt.commit(root, 0, 0);
        check(Nbt.get(before, "components", "minecraft:lore").equals(Nbt.get(root, "components", "minecraft:lore")),
                "commit(ed=0) lore'u tarif sonucuyla ayni olmali");
        check(Nbt.get(before, "components", "minecraft:equippable").equals(Nbt.get(root, "components", "minecraft:equippable")),
                "commit equippable (silent) tarif sonucuyla ayni olmali");
        check(Nbt.get(before, "components", "minecraft:custom_data").equals(Nbt.get(root, "components", "minecraft:custom_data")),
                "commit(ed=0) custom_data tarif sonucuyla ayni olmali");

        // gercek deger yazimi
        ElytraNbt.commit(root, 7, 100);
        check(ElytraNbt.damage(Nbt.components(root)) == 7, "damage 7");
        check(ElytraNbt.ed(Nbt.components(root)) == 100, "ae_ed 100");
        check(Nbt.get(root, "components", "minecraft:custom_data", "ae_ed") instanceof Integer, "ae_ed int olmali");
        List<Object> lore = Nbt.list(Nbt.get(root, "components", "minecraft:lore"));
        check(lore.size() == 3, "lore 3 satir kalmali");
        Map<String, Object> third = Nbt.map(lore.get(2));
        List<Object> extra = Nbt.list(third.get("extra"));
        check("332".equals(Nbt.map(extra.get(1)).get("text")), "432-100 = 332 yazilmali");
        check(Integer.valueOf(4900).equals(root.get("DataVersion")), "DataVersion korunmali");
        ElytraNbt.commit(root, 0, 100);
        check(!Nbt.components(root).containsKey("minecraft:damage"), "damage 0 ise bilesen kalkmali");

        // lore eksik/kisa olan eski esya
        Map<String, Object> old = Nbt.compound();
        Nbt.components(old).put("minecraft:lore", new ArrayList<Object>());
        ElytraNbt.commit(old, 1, 5);
        check(Nbt.list(Nbt.get(old, "components", "minecraft:lore")).size() == 1, "bos lore: sadece 1 bos satir eklenir (lore[2] yazilamaz)");

        // make_elytra
        Map<String, Object> ench = Nbt.compound();
        ench.put("minecraft:protection", Integer.valueOf(4));
        ench.put("minecraft:mending", Integer.valueOf(1));
        ench.put("minecraft:unbreaking", Integer.valueOf(3));
        ench.put("minecraft:feather_falling", Integer.valueOf(4));
        Nbt.components(root).put("minecraft:enchantments", ench);
        Map<String, Object> el = ElytraNbt.makeElytra(root, 431);
        Map<String, Object> ee = Nbt.map(Nbt.get(el, "components", "minecraft:enchantments"));
        check("minecraft:elytra".equals(el.get("id")), "elytra id");
        check(!ee.containsKey("minecraft:protection") && !ee.containsKey("minecraft:feather_falling"), "zirh buyuleri silinmeli");
        check(ee.containsKey("minecraft:mending") && ee.containsKey("minecraft:unbreaking"), "ortak buyuler kalmali");
        check(ElytraNbt.damage(Nbt.map(el.get("components"))) == 431, "elytra hasari 431");
        check(ench.containsKey("minecraft:protection"), "kaynak gogusluk degismemeli");

        ElytraNbt.toPlainChestplate(root);
        Map<String, Object> c = Nbt.components(root);
        check(!c.containsKey("minecraft:glider") && !c.containsKey("minecraft:equippable")
                && !c.containsKey("minecraft:lore") && !c.containsKey("minecraft:custom_data"), "sade gogusluk");
        check(c.containsKey("minecraft:enchantments"), "buyuler gogusluge kalmali");
        System.out.println("elytra NBT islemleri: kontroller gecti");
    }

    private static void testRefund() {
        check(DrillGeometry.refund(10, 500) == 9, "refund 10 -> 9");
        check(DrillGeometry.refund(1, 500) == 0, "refund 1 -> 0");
        check(DrillGeometry.refund(0, 500) == 0, "refund 0 -> 0");
        check(DrillGeometry.refund(499, 500) == 498, "refund 499 -> 498");
        System.out.println("hasar iadesi: kontroller gecti");
    }
}

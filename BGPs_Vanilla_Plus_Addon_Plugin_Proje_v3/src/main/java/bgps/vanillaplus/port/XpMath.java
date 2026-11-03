package bgps.vanillaplus.port;

/** armored_elytra: xp_total / xp_t1 / xp_t2 / xp_t3 (scoreboard tamsayi aritmetigi). */
public final class XpMath {
    private XpMath() {}

    /** Seviye + seviye ici puan -> toplam deneyim puani. */
    public static int total(int level, int points) {
        int xpt;
        if (level <= 16) {
            xpt = level * level + level * 6;
        } else if (level <= 31) {
            xpt = level * level * 5;
            xpt -= level * 81;
            xpt += 720;
            xpt = Math.floorDiv(xpt, 2);
        } else {
            xpt = level * level * 9;
            xpt -= level * 325;
            xpt += 4440;
            xpt = Math.floorDiv(xpt, 2);
        }
        return xpt + points;
    }
}

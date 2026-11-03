package bgps.vanillaplus.port;

/** mace_drill: plane_x / plane_y / plane_z ve yon secimi. */
public final class DrillGeometry {
    private DrillGeometry() {}

    public static final int PLANE_Y = 0; // yatay 3x3 (x,z)
    public static final int PLANE_X = 1; // dikey, x sabit (y,z)
    public static final int PLANE_Z = 2; // dikey, z sabit (x,y)

    /** Minecraft'in y_rotation degeri: [-180, 180) araligina sarilir. */
    public static float wrapDegrees(float d) {
        d = d % 360.0F;
        if (d >= 180.0F) d -= 360.0F;
        if (d < -180.0F) d += 360.0F;
        return d;
    }

    /** drilled.mcfunction'daki #plane hesabi. Sonraki kosullar oncekilerin ustune yazar. */
    public static int plane(float yaw, float pitch) {
        int plane = PLANE_Z;
        float y = wrapDegrees(yaw);
        if (y >= 45.0F && y <= 135.0F) plane = PLANE_X;
        if (y >= -135.0F && y <= -45.0F) plane = PLANE_X;
        if (pitch >= 45.0F && pitch <= 90.0F) plane = PLANE_Y;
        if (pitch >= -90.0F && pitch <= -45.0F) plane = PLANE_Y;
        return plane;
    }

    /** Ilgili plane_*.mcfunction dosyasindaki satir sirasiyla {dx, dy, dz} ofsetleri. */
    public static int[][] offsets(int plane) {
        int[][] out = new int[9][];
        int k = 0;
        if (plane == PLANE_Y) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) out[k++] = new int[]{dx, 0, dz};
        } else if (plane == PLANE_X) {
            for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) out[k++] = new int[]{0, dy, dz};
        } else {
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) out[k++] = new int[]{dx, dy, 0};
        }
        return out;
    }

    /** Eski sistemin item_modifier "refund" (set_damage +0.0019, add) islemi: hasari azaltir. */
    public static int refund(int damage, int maxDamage) {
        if (maxDamage <= 0) return damage;
        float f = 1.0F - (float) damage / (float) maxDamage;
        float sum = 0.0019F + f;
        if (sum < 0.0F) sum = 0.0F;
        if (sum > 1.0F) sum = 1.0F;
        float f1 = 1.0F - sum;
        return (int) Math.floor(f1 * (float) maxDamage);
    }
}

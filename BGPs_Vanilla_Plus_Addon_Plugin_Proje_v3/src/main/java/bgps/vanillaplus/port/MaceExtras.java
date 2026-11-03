package bgps.vanillaplus.port;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** mace_extras:smash - yuksekten dusup gurzle vurunca etraftaki varliklari yavaslatir ve zayiflatir. */
public final class MaceExtras implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        Player p = null;
        if (e.getDamager() instanceof Player pl) {
            p = pl;
        } else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player pl2) {
            p = pl2;
        }
        if (p == null) return;
        if (p.getInventory().getItemInMainHand().getType() != Material.MACE) return;

        int fall = (int) (p.getFallDistance() * 10.0);                          // data get entity @s fall_distance 10
        if (fall < 16) return;

        Location loc = p.getLocation();
        World w = loc.getWorld();
        w.spawnParticle(Particle.EXPLOSION, loc.clone().add(0.0, 0.2, 0.0), 8, 1.5, 0.1, 1.5, 0.0);
        w.spawnParticle(Particle.CRIT, loc.clone().add(0.0, 0.5, 0.0), 40, 2.0, 0.3, 2.0, 0.2);
        w.playSound(loc, "minecraft:item.mace.smash_ground_heavy", SoundCategory.PLAYERS, 1.0F, 1.0F);

        for (Entity en : w.getNearbyEntities(loc, 4.0, 4.0, 4.0)) {
            if (en == p) continue;
            if (en instanceof Item || en instanceof ExperienceOrb) continue;
            if (en.getLocation().distanceSquared(loc) > 16.0) continue;         // distance=..4
            if (en instanceof LivingEntity le) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 1));
            }
        }
    }
}

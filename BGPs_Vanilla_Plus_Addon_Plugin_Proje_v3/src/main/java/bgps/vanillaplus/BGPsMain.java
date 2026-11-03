package bgps.vanillaplus;

import bgps.vanillaplus.port.ArmoredElytra;
import bgps.vanillaplus.port.GiveCommand;
import bgps.vanillaplus.port.MaceDrill;
import bgps.vanillaplus.port.MaceExtras;
import bgps.vanillaplus.port.Recipes;
import java.util.logging.Level;
import org.bukkit.Bukkit;

/**
 * Yeni ana sinif. Mevcut BGPsVanillaPlusAddon (demirci masasi korumasi + tarif kitabi) ayni kalir:
 * super.onEnable() onu oldugu gibi calistirir. Ustune, eskiden data pack olan sistemler eklenir.
 */
public final class BGPsMain extends BGPsVanillaPlusAddon {
    private MaceDrill drill;
    private ArmoredElytra elytra;

    @Override
    public void onEnable() {
        super.onEnable();

        try {
            Recipes.registerAll(this);
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Tarifler kaydedilemedi", t);
        }

        drill = new MaceDrill(this);
        elytra = new ArmoredElytra(this);
        Bukkit.getPluginManager().registerEvents(drill, this);
        Bukkit.getPluginManager().registerEvents(new MaceExtras(), this);
        Bukkit.getPluginManager().registerEvents(elytra, this);
        GiveCommand.register(this);

        // eski data pack'in #minecraft:tick fonksiyonlari: mace_drill:tick, sonra armored_elytra:tick
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            drill.tick();
            elytra.tick();
        }, 1L, 1L);

        getLogger().info("Data pack mantigi plugin icine tasindi (mace_drill, mace_extras, armored_elytra).");
    }

    @Override
    public void onDisable() {
        Recipes.unregisterAll();
    }
}

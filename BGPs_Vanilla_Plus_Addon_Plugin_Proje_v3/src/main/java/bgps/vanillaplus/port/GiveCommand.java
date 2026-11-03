package bgps.vanillaplus.port;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/** Eski `function mace_drill:give/*` fonksiyonlarinin karsiligi: /bgpsgive <esya|all> [oyuncu] */
public final class GiveCommand extends Command {
    private static final String PERM = "bgps.give";
    private static final String[] ITEMS = {
            "mace", "book", "template", "elytra_leather", "elytra_chainmail", "elytra_iron",
            "elytra_copper", "elytra_golden", "elytra_diamond", "elytra_netherite"
    };

    private GiveCommand() {
        super("bgpsgive", "BGP's Vanilla+ Addon esyalarini verir", "/bgpsgive <esya|all> [oyuncu]", new ArrayList<>());
        setPermission(PERM);
    }

    public static void register(Plugin plugin) {
        plugin.getServer().getCommandMap().register("bgps", new GiveCommand());
    }

    static ItemStack make(String name) {
        switch (name) {
            case "mace": {                                    // give/mace: tarif sonucu + 3500 can
                Map<String, Object> root = ItemNbt.read(Recipes.result("mace_drill", "mace_drill"));
                HpLore.apply(Nbt.components(root), 3500);
                return ItemNbt.write(root);
            }
            case "book": return Recipes.result("mace_drill", "drill_book");
            case "template": return Recipes.result("mace_drill", "rainbow_template");
            default:
                if (name.startsWith("elytra_")) return Recipes.result("armored_elytra", name.substring(7) + "_chestplate_glider");
                return null;
        }
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage("Bu komut icin yetkin yok.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage("Kullanim: /bgpsgive <" + String.join("|", ITEMS) + "|all> [oyuncu]");
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) { sender.sendMessage("Oyuncu bulunamadi: " + args[1]); return true; }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage("Konsoldan bir oyuncu adi belirtmelisin.");
            return true;
        }

        String which = args[0].toLowerCase(Locale.ROOT);
        List<String> names = new ArrayList<>();
        if (which.equals("all")) {
            for (String n : ITEMS) names.add(n);
        } else {
            names.add(which);
        }
        for (String n : names) {
            ItemStack it = make(n);
            if (it == null) { sender.sendMessage("Bilinmeyen esya: " + n); return true; }
            for (ItemStack left : target.getInventory().addItem(it).values()) {
                target.getWorld().dropItem(target.getLocation(), left);
            }
        }
        sender.sendMessage(target.getName() + " oyuncusuna verildi: " + which);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String n : ITEMS) if (n.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(n);
            if ("all".startsWith(args[0].toLowerCase(Locale.ROOT))) out.add("all");
        } else if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
        }
        return out;
    }
}

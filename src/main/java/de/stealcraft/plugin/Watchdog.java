package de.stealcraft.plugin;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Runs twice a second:
 * - pulls bench items back up if they fall into the void,
 * - notices if a placed bench vanished without being mined (e.g. by a command) and tells admins.
 */
public final class Watchdog implements Runnable {

    private final StealCraftPlugin plugin;

    public Watchdog(StealCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (World world : Bukkit.getWorlds()) {
            int floor = world.getMinHeight();
            for (Item item : world.getEntitiesByClass(Item.class)) {
                if (item.getLocation().getY() > floor - 1) continue;
                if (!plugin.benchItem().isBench(item.getItemStack())) continue;
                Location l = item.getLocation();
                int x = l.getBlockX(), z = l.getBlockZ();
                int y = world.getHighestBlockYAt(x, z);
                Location safe = (y <= floor) ? world.getSpawnLocation() : new Location(world, x + 0.5, y + 1.5, z + 0.5);
                item.setVelocity(new Vector(0, 0, 0));
                item.teleport(safe);
            }
        }

        for (TeamData t : plugin.store().teams()) {
            if (!t.placed() || t.location() == null || t.location().getWorld() == null) continue;
            Location l = t.location();
            if (!l.getWorld().isChunkLoaded(l.getBlockX() >> 4, l.getBlockZ() >> 4)) continue;
            if (l.getBlock().getType() == Material.CRAFTING_TABLE) continue;

            t.placed(false);
            t.location(null);
            plugin.store().save();
            plugin.tab().refresh();
            plugin.getLogger().warning("Die Werkbank von Team " + t.id() + " ist verschwunden, ohne abgebaut zu werden.");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission("stealcraft.admin")) {
                    p.sendMessage(Game.msg("Die Werkbank von Team " + t.id()
                            + " ist verschwunden (z. B. durch einen Befehl). Gib ihnen mit /sc werkbank " + t.id() + " eine neue.", NamedTextColor.GOLD));
                }
            }
        }
    }
}

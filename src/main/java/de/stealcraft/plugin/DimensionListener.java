// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 IT-Tedo and StealCraft contributors
package de.stealcraft.plugin;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.EnumSet;
import java.util.Set;

/**
 * Blocks entering the Nether or the End while an operator has locked it (/sc nether sperren, /sc end sperren).
 * Leaving them stays possible, so a player who is already inside can still get out.
 */
public final class DimensionListener implements Listener {

    private static final Set<TeleportCause> PORTAL_CAUSES =
            EnumSet.of(TeleportCause.NETHER_PORTAL, TeleportCause.END_PORTAL, TeleportCause.END_GATEWAY);

    private final StealCraftPlugin plugin;

    public DimensionListener(StealCraftPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent e) {
        if (denied(e.getFrom(), e.getTo(), e.getCause())) {
            e.setCancelled(true);
            notify(e.getPlayer(), e.getFrom(), e.getTo(), e.getCause());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (!PORTAL_CAUSES.contains(e.getCause())) return;
        if (denied(e.getFrom(), e.getTo(), e.getCause())) {
            e.setCancelled(true);
            notify(e.getPlayer(), e.getFrom(), e.getTo(), e.getCause());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobPortal(EntityPortalEvent e) {
        if (denied(e.getFrom(), e.getTo(), null)) e.setCancelled(true);
    }

    private boolean denied(Location from, Location to, TeleportCause cause) {
        World.Environment target = targetOf(from, to, cause);
        if (target == null) return false;
        return (target == World.Environment.NETHER || target == World.Environment.THE_END)
                && plugin.store().isLocked(target);
    }

    private void notify(Player p, Location from, Location to, TeleportCause cause) {
        World.Environment target = targetOf(from, to, cause);
        if (target == World.Environment.NETHER) {
            p.sendMessage(Game.msg("Das Nether ist gesperrt. Ein Operator öffnet es mit /sc nether öffnen.", NamedTextColor.RED));
        } else if (target == World.Environment.THE_END) {
            p.sendMessage(Game.msg("Das End ist gesperrt. Ein Operator öffnet es mit /sc end öffnen.", NamedTextColor.RED));
        }
    }

    /** The dimension the player would arrive in. Uses the destination if known, otherwise guesses from the portal type. */
    private static World.Environment targetOf(Location from, Location to, TeleportCause cause) {
        if (to != null && to.getWorld() != null) return to.getWorld().getEnvironment();
        if (from == null || from.getWorld() == null || cause == null) return null;
        World.Environment here = from.getWorld().getEnvironment();
        return switch (cause) {
            case NETHER_PORTAL -> here == World.Environment.NETHER ? World.Environment.NORMAL : World.Environment.NETHER;
            case END_PORTAL, END_GATEWAY -> here == World.Environment.THE_END ? World.Environment.NORMAL : World.Environment.THE_END;
            default -> null;
        };
    }
}
